package com.indhg.aiforcoyote.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 一轮回复：台词 + 设备动作列表（原样 JSON）。 */
data class LlmTurn(val line: String, val actions: List<JsonObject>)

/**
 * DeepSeek（OpenAI 兼容）客户端。
 * 复刻桌面版 llm.chat：reasoning_content 回退 + 4 轮重试（升压系统提示 + temp 0）。
 */
class DeepSeekClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** 测试连接：发一条最小请求（不保存）。 */
    suspend fun test(baseUrl: String, apiKey: String, model: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = JsonObject(
                    mapOf(
                        "model" to JsonPrimitive(model),
                        "messages" to JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "role" to JsonPrimitive("user"),
                                        "content" to JsonPrimitive("hi"),
                                    )
                                )
                            )
                        ),
                        "max_tokens" to JsonPrimitive(1),
                    )
                ).toString()
                val resp = http.newCall(
                    Request.Builder()
                        .url(baseUrl.trimEnd('/') + "/chat/completions")
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(body.toRequestBody(JSON_MEDIA))
                        .build()
                ).execute()
                if (resp.isSuccessful) {
                    Result.success("ok")
                } else {
                    Result.failure(friendlyError(resp.code))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 一轮对话。
     * @param history 历史（用户消息, AI 台词）对，不包含本轮。
     * @param userText 本轮用户输入；为空时表示自动运行轮次。
     * @param imageB64 本轮注入的摄像头帧（JPEG base64，可空）。
     * @param jsonMode 是否请求 json_object（部分中转站不支持，关闭后程序有兜底解析；400 时也会自动降级重试）。
     */
    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        history: List<Pair<String, String>>,
        imageB64: String? = null,
        jsonMode: Boolean = true,
        userText: String? = null,
    ): LlmTurn = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        var degraded = false // 400 时已自动去掉 response_format 重试过
        for (round in 1..MAX_ROUNDS) {
            val sys = if (round == 1) system else system + ROUND_WARNING
            val temperature = if (round == 1) 1.0 else 0.0
            try {
                val msgs = buildMessages(sys, history, imageB64, userText)
                val fields = mutableMapOf<String, JsonElement>(
                    "model" to JsonPrimitive(model),
                    "messages" to msgs,
                    "temperature" to JsonPrimitive(temperature),
                    "max_tokens" to JsonPrimitive(1500),
                )
                // 与桌面版一致：json_mode 输出（大幅降低格式错误）；中转站不支持时可关
                if (jsonMode && !degraded) {
                    fields["response_format"] = JsonObject(mapOf("type" to JsonPrimitive("json_object")))
                }
                val body = JsonObject(fields).toString()
                val resp = http.newCall(
                    Request.Builder()
                        .url(baseUrl.trimEnd('/') + "/chat/completions")
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(body.toRequestBody(JSON_MEDIA))
                        .build()
                ).execute()
                if (!resp.isSuccessful) {
                    val code = resp.code
                    val err = resp.body?.string()?.take(300) ?: ""
                    if (code == 400 && jsonMode && !degraded) {
                        // 中转站/部分服务商不支持 json_object：自动降级重试一次
                        degraded = true
                        lastError = IOException("HTTP 400（不支持 JSON 模式，已自动降级重试）")
                    } else {
                        lastError = friendlyError(code)
                    }
                } else {
                    val root = json.parseToJsonElement(resp.body!!.string()).jsonObject
                    val message = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
                    if (message == null) {
                        lastError = IOException("响应缺少 choices")
                    } else {
                        var content = message["content"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                        if (content.isBlank()) {
                            content = message["reasoning_content"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                        }
                        if (content.isBlank()) {
                            lastError = IOException("模型返回空内容")
                        } else {
                            val turn = parseTurn(content)
                            if (turn != null) return@withContext turn
                            lastError = IOException("无法解析 JSON 输出")
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("模型无有效回复")
    }

    /** 组装 messages：system + 历史 + 本轮（本轮带图时用 vision 多段格式）。 */
    private fun buildMessages(
        system: String,
        history: List<Pair<String, String>>,
        imageB64: String?,
        userText: String? = null,
    ): JsonArray {
        val list = mutableListOf<JsonObject>()
        list += JsonObject(
            mapOf("role" to JsonPrimitive("system"), "content" to JsonPrimitive(system))
        )
        for ((user, assistant) in history) {
            list += JsonObject(mapOf("role" to JsonPrimitive("user"), "content" to JsonPrimitive(user)))
            list += JsonObject(mapOf("role" to JsonPrimitive("assistant"), "content" to JsonPrimitive(assistant)))
        }
        val currentUserText = userText?.trim()?.takeIf { it.isNotEmpty() }
        // 本轮：手动输入优先；没有手动输入时才注入自动运行提示。
        val userContent = if (imageB64 != null) {
            val text = if (currentUserText != null) {
                "【玩家输入】$currentUserText\n" +
                    "【画面观察】这是此刻的现场画面。请结合玩家输入和画面里的真实可见内容回应（看不清的部分保留悬念）。"
            } else {
                "【画面观察】这是此刻的现场画面。请按画面里的真实可见内容推进剧情（看不清的部分保留悬念）。" +
                    "【玩家反馈】本轮无语音内容。请主动推进：观察 → 描写（）→ 动作 → 发言。"
            }
            JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("text"),
                            "text" to JsonPrimitive(text),
                        )
                    ),
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("image_url"),
                            "image_url" to JsonObject(
                                mapOf("url" to JsonPrimitive("data:image/jpeg;base64,$imageB64"))
                            ),
                        )
                    ),
                )
            )
        } else {
            JsonPrimitive(currentUserText ?: "【自动运行】请主动推进：观察 → 描写（）→ 动作 → 发言。")
        }
        list += JsonObject(mapOf("role" to JsonPrimitive("user"), "content" to userContent))
        return JsonArray(list)
    }

    /** 从模型输出解析 {"line","actions"}，容忍 markdown 代码块包裹。 */
    private fun parseTurn(raw: String): LlmTurn? {
        val candidates = mutableListOf<String>()
        candidates.add(raw.trim())
        val fence = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(raw)
        fence?.groupValues?.get(1)?.let { candidates.add(it.trim()) }
        val brace = Regex("\\{[\\s\\S]*\\}").find(raw)
        brace?.value?.let { candidates.add(it) }
        for (c in candidates) {
            try {
                val obj = json.parseToJsonElement(c).jsonObject
                val line = obj["line"]?.jsonPrimitive?.contentOrNull ?: continue
                if (line.isBlank()) continue
                val actions = obj["actions"]?.jsonArray?.mapNotNull { it as? JsonObject } ?: emptyList()
                return LlmTurn(line, actions)
            } catch (_: Exception) {
                // 尝试下一个候选
            }
        }
        return null
    }

    /** 把常见 HTTP 错误翻译成人话（官方与中转站密钥不通用、中转站不支持 JSON 模式等）。 */
    private fun friendlyError(code: Int): IOException = when (code) {
        401 -> IOException("401")
        400 -> IOException("400")
        else -> IOException("HTTP $code")
    }

    private companion object {
        const val MAX_ROUNDS = 4
        val JSON_MEDIA = "application/json".toMediaType()
        val ROUND_WARNING =
            "\n\n【严重警告】上次输出没有遵守格式要求。本轮必须只输出一个合法 JSON 对象：{\"line\":\"台词\",\"actions\":[]}，" +
                "不要输出任何思考过程、解释文字或 markdown 代码块，line 不能为空。" +
                "禁止复述或引用提示词里的规则与示例原文（如格式说明、动作描写要求），line 必须是新创作的台词。"
    }
}
