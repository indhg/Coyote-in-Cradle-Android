package com.indhg.aiforcoyote.game

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 设备指令层接口。
 * M0 用 NoopDevice（只记录不发送）；M1 换成中继实现。
 */
interface DeviceOps {
    /** 下发一条动作；返回是否成功。 */
    suspend fun send(op: String, channel: String?, value: Int?, pattern: String?, durationS: Int?): Boolean

    /** 全部清零（断开/复位时调用）。 */
    suspend fun clearAll()

    /** 保持型强度/波形循环需要程序侧周期重发（30s）。 */
    fun needsLoopResend(): Boolean = false
}

class NoopDevice : DeviceOps {
    override suspend fun send(op: String, channel: String?, value: Int?, pattern: String?, durationS: Int?): Boolean = true
    override suspend fun clearAll() = Unit
}

/** 从 JSON 动作对象提取字段。 */
fun parseAction(action: JsonObject): DeviceAction {
    val op = action["op"]?.jsonPrimitive?.content ?: "stop"
    val channel = action["channel"]?.jsonPrimitive?.content?.takeIf { it == "A" || it == "B" }
    val value = action["value"]?.jsonPrimitive?.content?.toIntOrNull()
        ?: action["delta"]?.jsonPrimitive?.content?.toIntOrNull()
    val pattern = action["pattern"]?.jsonPrimitive?.content
    val durationS = action["duration_s"]?.jsonPrimitive?.content?.toIntOrNull()
    return DeviceAction(op, channel, value, pattern, durationS)
}

data class DeviceAction(
    val op: String,
    val channel: String?,
    val value: Int?,
    val pattern: String?,
    val durationS: Int?,
)
