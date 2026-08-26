package com.indhg.aiforcoyote.game

import android.content.Context
import android.util.Log
import com.indhg.aiforcoyote.relay.CoyoteController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicInteger

/**
 * 真实设备指令层：把安全层命令转成 v4 RPC 帧，经控制方客户端下发。
 * 复刻桌面版 device_ops.py：t=0 波形 / t=3 增减 / t=4 临时 / t=7 归零；m=device.op.clear 清理。
 */
class RelayDevice(
    context: Context,
    private val controller: CoyoteController,
    private val scope: CoroutineScope,
) : DeviceOps {

    private val json = Json { ignoreUnknownKeys = true }
    private val reqCounter = AtomicInteger(1)
    private val local = mutableMapOf("A" to 0, "B" to 0)

    /** 当前保持中的波形循环（通道, 波形名），用于 30s 重发。 */
    private var pulseHold: Pair<String, String>? = null

    private data class WaveData(val frames: List<String>, val defaultDurationS: Int)

    private val waveByName: Map<String, WaveData> by lazy { loadWaves(context) }

    private fun loadWaves(context: Context): Map<String, WaveData> {
        return try {
            val text = context.assets.open("waveforms.json").bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(text).jsonObject
            root.entries.mapNotNull { (name, v) ->
                val obj = v.jsonObject
                val frames = obj["frames"]?.jsonArray?.map { it.jsonPrimitive.content } ?: return@mapNotNull null
                name to WaveData(
                    frames,
                    (obj["default_duration_s"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()?.toInt()) ?: 6,
                )
            }.toMap()
        } catch (e: Exception) {
            Log.w(TAG, "波形数据加载失败: ${e.message}")
            emptyMap()
        }
    }

    override suspend fun send(op: String, channel: String?, value: Int?, pattern: String?, durationS: Int?): Boolean {
        val ch = channel ?: return false
        return when (op) {
            "hold_strength" -> {
                val target = (value ?: 0).coerceIn(0, 100)
                val delta = target - (local[ch] ?: 0)
                if (delta != 0) sendReq("device.op", mapOf("s" to slotOrNull(), "c" to chIdx(ch), "t" to 3, "v" to delta))
                local[ch] = target
                true
            }
            "temp_strength" -> {
                val v = (value ?: 0).coerceIn(0, 100)
                val d = ((durationS ?: 3).coerceIn(3, 10)) * 1000
                sendReq("device.op", mapOf("s" to slotOrNull(), "c" to chIdx(ch), "t" to 4, "v" to v, "d" to d))
                local[ch] = v
                scope.launch { delay(d.toLong()); if (local[ch] == v) local[ch] = 0 }
                true
            }
            "add_strength" -> {
                val delta = value ?: return false
                sendReq("device.op", mapOf("s" to slotOrNull(), "c" to chIdx(ch), "t" to 3, "v" to delta))
                local[ch] = ((local[ch] ?: 0) + delta).coerceIn(0, 100)
                true
            }
            "pulse_hold" -> {
                val p = pattern ?: return false
                pulseHold = ch to p
                sendWave(ch, p, LOOP_BATCH_MS)
            }
            "pulse" -> {
                val p = pattern ?: return false
                sendWave(ch, p, ((durationS ?: 6).coerceIn(3, 10)) * 1000)
            }
            "clear" -> {
                sendReq("device.op.clear", mapOf("s" to slotOrNull(), "c" to chIdx(ch)))
                local[ch] = 0
                if (pulseHold?.first == ch) pulseHold = null
                true
            }
            else -> false
        }
    }

    override suspend fun clearAll() {
        sendReq("device.op.clear", null)
        local["A"] = 0
        local["B"] = 0
        pulseHold = null
    }

    /** 波形循环：每 30s 重发当前保持中的波形。 */
    override fun needsLoopResend(): Boolean = pulseHold != null

    suspend fun resendLoop() {
        val (ch, p) = pulseHold ?: return
        sendWave(ch, p, LOOP_BATCH_MS)
    }

    private fun sendWave(ch: String, pattern: String, durationMs: Int): Boolean {
        val w = waveByName[pattern] ?: run {
            Log.w(TAG, "未知波形: $pattern")
            return false
        }
        return sendReq(
            "device.op",
            mapOf("s" to slotOrNull(), "c" to chIdx(ch), "t" to 0, "v" to JsonArray(w.frames.map { JsonPrimitive(it) }), "d" to durationMs, "im" to true),
        )
    }

    private fun sendReq(method: String, data: Map<String, Any?>?): Boolean {
        val inner = buildJsonObject {
            put("t", JsonPrimitive("req"))
            put("reqId", JsonPrimitive(reqCounter.getAndIncrement().toString()))
            put("m", JsonPrimitive(method))
            data?.forEach { (k, v) ->
                when (v) {
                    null -> put(k, kotlinx.serialization.json.JsonNull)
                    is String -> put(k, JsonPrimitive(v))
                    is Int -> put(k, JsonPrimitive(v))
                    is Boolean -> put(k, JsonPrimitive(v))
                    is JsonArray -> put(k, v)
                    else -> put(k, JsonPrimitive(v.toString()))
                }
            }
        }
        return controller.sendData(inner)
    }

    private fun slotOrNull(): String? = controller.state.value.slotId

    private fun chIdx(ch: String): Int = if (ch == "A") 0 else 1

    companion object {
        private const val TAG = "RelayDevice"
        private const val LOOP_BATCH_MS = 30_000
    }
}
