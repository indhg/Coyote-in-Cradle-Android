package com.indhg.aiforcoyote.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
                    Result.success("连接成功，模型可用")
                } else {
                    val err = resp.body?.string()?.take(300) ?: ""
                    Result.failure(IOException("HTTP ${resp.code}: $err"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 一轮对话。
     * @param history 历史（用户消息, AI 台词）对，不包含本轮。
     * @param imageB64 本轮注入的摄像头帧（JPEG base64，可空）。
     */
    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        history: List<Pair<String, String>>,
        imageB64: String? = null,
    ): LlmTurn = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (round in 1..MAX_ROUNDS) {
            val sys = if (round == 1) system else system + ROUND_WARNING
            val temperature = if (round == 1) 1.0 else 0.0
            try {
                val msgs = buildMessages(sys, history, imageB64)
                val body = JsonObject(
                    mapOf(
                        "model" to JsonPrimitive(model),
                        "messages" to msgs,
                        "temperature" to JsonPrimitive(temperature),
                        "max_tokens" to JsonPrimitive(1500),
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
                if (!resp.isSuccessful) {
                    val err = resp.body?.string()?.take(300) ?: ""
                    lastError = IOException("HTTP ${resp.code}: $err")
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
    ): JsonArray {
        val list = mutableListOf<JsonObject>()
        list += JsonObject(
            mapOf("role" to JsonPrimitive("system"), "content" to JsonPrimitive(system))
        )
        for ((user, assistant) in history) {
            list += JsonObject(mapOf("role" to JsonPrimitive("user"), "content" to JsonPrimitive(user)))
            list += JsonObject(mapOf("role" to JsonPrimitive("assistant"), "content" to JsonPrimitive(assistant)))
        }
        // 本轮：自动运行轮次注入观察信号；用户消息轮次注入用户文本
        val userContent = if (imageB64 != null) {
            JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("text"),
                            "text" to JsonPrimitive(
                                "【画面观察】这是此刻的现场画面。请按画面里的真实可见内容推进剧情（看不清的部分保留悬念）。" +
                                    "【玩家反馈】本轮无语音内容。请主动推进：观察 → 描写（）→ 动作 → 发言。"
                            ),
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
            JsonPrimitive("【自动运行】请主动推进：观察 → 描写（）→ 动作 → 发言。")
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

    private companion object {
        const val MAX_ROUNDS = 4
        val JSON_MEDIA = "application/json".toMediaType()
        val ROUND_WARNING =
            "\n\n【严重警告】上次输出没有遵守格式要求。本轮必须只输出一个合法 JSON 对象：{\"line\":\"台词\",\"actions\":[]}，" +
                "不要输出任何思考过程、解释文字或 markdown 代码块，line 不能为空。"
    }
}
