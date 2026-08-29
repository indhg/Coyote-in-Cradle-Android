package com.indhg.aiforcoyote.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 安全层：任何来源（AI/手动）命令的唯一出口。
 * 复刻桌面版 safety.py：每通道上限 100、单次变化 ≤40、过热降 20、断开清零。
 */
class Safety(private val device: DeviceOps) {

    data class Executed(val label: String)
    data class Dropped(val reason: String)

    private val _strengths = MutableStateFlow(mapOf("A" to 0, "B" to 0))
    val strengths: StateFlow<Map<String, Int>> = _strengths.asStateFlow()

    // 硬上限 = 郊狼满值 200；运行时上限默认 100（页面可调 1~200，不持久化）
    private val hardCaps = mapOf("A" to 200, "B" to 200)
    private val userCaps = mutableMapOf("A" to 100, "B" to 100)
    private val _caps = MutableStateFlow(mapOf("A" to 100, "B" to 100))
    val caps: StateFlow<Map<String, Int>> = _caps.asStateFlow()

    var overheat = false
        set(value) {
            field = value
            publishCaps()
        }

    /** 通道生效上限：min(硬上限, 用户上限, 过热 20)。 */
    fun capFor(ch: String): Int {
        val hard = hardCaps[ch] ?: 200
        val user = userCaps[ch] ?: 100
        return min(hard, if (overheat) min(user, 20) else user)
    }

    /** 调整通道运行时上限（1~200）。 */
    fun setUserCap(ch: String, value: Int) {
        val v = value.coerceIn(1, hardCaps[ch] ?: 200)
        userCaps[ch] = v
        val cur = _strengths.value[ch] ?: 0
        if (cur > v) _strengths.value = _strengths.value + (ch to v)
        publishCaps()
    }

    private fun publishCaps() {
        _caps.value = mapOf("A" to capFor("A"), "B" to capFor("B"))
    }

    /** 应用一批 AI 动作；返回执行/丢弃记录。 */
    suspend fun apply(actions: List<DeviceAction>): Pair<List<Executed>, List<Dropped>> {
        val executed = mutableListOf<Executed>()
        val dropped = mutableListOf<Dropped>()
        for (a in actions) {
            when (a.op) {
                "stop" -> {
                    _strengths.value = mapOf("A" to 0, "B" to 0)
                    device.clearAll()
                    executed += Executed("全部停止")
                }
                "clear" -> {
                    val ch = a.channel
                    if (ch != null) {
                        _strengths.value = _strengths.value + (ch to 0)
                        device.send("clear", ch, 0, null, null)
                        executed += Executed("$ch 清零")
                    } else {
                        _strengths.value = mapOf("A" to 0, "B" to 0)
                        device.clearAll()
                        executed += Executed("全部清零")
                    }
                }
                "hold_strength", "temp_strength" -> {
                    val ch = a.channel
                    val v = a.value
                    if (ch == null || v == null) {
                        dropped += Dropped("缺少通道或数值")
                        continue
                    }
                    val cap = capFor(ch)
                    val cur = _strengths.value[ch] ?: 0
                    if (abs(v - cur) > MAX_STEP) {
                        dropped += Dropped("$ch 单次变化 ${abs(v - cur)} 超过 $MAX_STEP")
                        continue
                    }
                    val clamped = max(0, min(cap, v))
                    _strengths.value = _strengths.value + (ch to clamped)
                    device.send(a.op, ch, clamped, null, a.durationS)
                    executed += Executed("$ch 强度 $clamped")
                }
                "add_strength" -> {
                    val ch = a.channel
                    if (ch == null) {
                        dropped += Dropped("缺少通道")
                        continue
                    }
                    val delta = a.value
                    if (delta == null) {
                        dropped += Dropped("缺少 delta")
                        continue
                    }
                    val cap = capFor(ch)
                    val cur = _strengths.value[ch] ?: 0
                    val clamped = max(0, min(cap, cur + delta))
                    _strengths.value = _strengths.value + (ch to clamped)
                    device.send("add_strength", ch, delta, null, null)
                    executed += Executed("$ch 强度 $clamped")
                }
                "pulse_hold", "pulse" -> {
                    val ch = a.channel
                    val p = a.pattern
                    if (ch == null || p == null) {
                        dropped += Dropped("缺少通道或波形名")
                        continue
                    }
                    device.send(a.op, ch, null, p, a.durationS)
                    executed += Executed("$ch 波形 $p")
                }
                else -> dropped += Dropped("未知 op ${a.op}")
            }
        }
        return executed to dropped
    }

    /** 断开/复位：清零并通知设备。 */
    suspend fun reset() {
        _strengths.value = mapOf("A" to 0, "B" to 0)
        device.clearAll()
    }

    companion object {
        const val MAX_STEP = 40
    }
}
