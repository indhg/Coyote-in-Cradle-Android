package com.indhg.aiforcoyote

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.indhg.aiforcoyote.data.Settings
import com.indhg.aiforcoyote.data.SettingsRepository
import com.indhg.aiforcoyote.game.AudioObserver
import com.indhg.aiforcoyote.game.AudioState
import com.indhg.aiforcoyote.game.BleCoyote
import com.indhg.aiforcoyote.game.CameraObserver
import com.indhg.aiforcoyote.game.CameraState
import com.indhg.aiforcoyote.game.DeviceAction
import com.indhg.aiforcoyote.game.DeviceState
import com.indhg.aiforcoyote.game.Safety
import com.indhg.aiforcoyote.game.parseAction
import com.indhg.aiforcoyote.llm.DeepSeekClient
import com.indhg.aiforcoyote.llm.SystemPrompt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 聊天消息（UI 层）。 */
data class UiMsg(val role: String, val text: String, val note: String = "")

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)
    private val client = DeepSeekClient()

    private var safetyRef: Safety? = null
    private val ble = BleCoyote(app, viewModelScope, onDisconnect = { viewModelScope.launch { safetyRef?.reset() } })
    private val safety = Safety(ble)

    val deviceState: StateFlow<DeviceState> = ble.state

    // M2：观察闭环（摄像头截帧 + 麦克风音量分级）
    private val camera = CameraObserver(app)
    private val audio = AudioObserver(app, viewModelScope, onMoan = { kind, _ -> addNote(moanText(kind)) })
    val cameraState: StateFlow<CameraState> = camera.state
    val audioState: StateFlow<AudioState> = audio.state
    private val notes = mutableListOf<String>()
    private var rageRounds = 0

    // M3：双通道保底（轮次计数 + 每通道最近一次强度/波形调整轮次）
    private var turnCount = 0
    private val lastStrength = mutableMapOf("A" to 0, "B" to 0)
    private val lastWave = mutableMapOf("A" to 0, "B" to 0)

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _messages = MutableStateFlow<List<UiMsg>>(emptyList())
    val messages: StateFlow<List<UiMsg>> = _messages.asStateFlow()

    val strengths: StateFlow<Map<String, Int>> = safety.strengths

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val history = mutableListOf<Pair<String, String>>()
    private var loopJob: Job? = null

    init {
        viewModelScope.launch {
            repo.settings.collect { s ->
                val autopilotChanged = s.autopilot != _settings.value.autopilot
                _settings.value = s
                if (autopilotChanged) restartLoop()
            }
        }
        viewModelScope.launch { restartLoop() }

        // BLE 直连郊狼（断开时安全层自动清零）
        safetyRef = safety
    }

    fun connectDevice() {
        ble.connect()
    }

    fun disconnectDevice() {
        ble.disconnect()
    }

    /** 前台开始观察（摄像头 + 麦克风）；权限缺失的自动降级不崩溃。 */
    fun startObservation(owner: LifecycleOwner) {
        camera.start(owner)
        audio.start()
    }

    /** 退后台暂停观察。 */
    fun stopObservation() {
        camera.stop()
        audio.stop()
    }

    private fun addNote(text: String) {
        notes += text
        if (notes.size > 5) notes.removeAt(0)
    }

    private fun moanText(kind: String): String = if (kind == "high") {
        "麦克风检测到玩家发出较大的呻吟/惨叫（音量高）：应降低强度、安抚并关心，不要继续加码。"
    } else {
        "麦克风检测到玩家发出普通呻吟/呜呜声（音量中等）：挑逗等级可逐渐增加，小幅加码。"
    }

    fun clearToast() {
        _toast.value = null
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun toggleAutopilot() {
        updateSettings { it.copy(autopilot = !it.autopilot) }
    }

    fun testConnection(apiKey: String, baseUrl: String, model: String, onDone: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            val r = client.test(baseUrl, apiKey, model)
            r.onSuccess { onDone(it, true) }
            r.onFailure { onDone(it.message ?: "连接失败", false) }
        }
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        _messages.value = _messages.value + UiMsg("user", t)
        viewModelScope.launch { turn(userText = t) }
    }

    private suspend fun turn(userText: String? = null) {
        if (_busy.value) return
        _busy.value = true
        val s = _settings.value
        try {
            if (s.apiKey.isBlank()) {
                _toast.value = "请先在设置页填写 API Key"
                _busy.value = false
                return
            }
            // M2：观察信号——怒气值（画面黑暗 / 持续无声）+ 最新帧注入 + 呻吟反馈
            val cs = camera.state.value
            val asSt = audio.state.value
            var rage = false
            if (cs.enabled) rage = rage || (cs.hasFrame && cs.dark)
            if (asSt.enabled) rage = rage || asSt.silent
            if (rage) rageRounds++ else rageRounds = 0
            val img = if (cs.enabled && cs.hasFrame) camera.base64() else null
            val system = SystemPrompt.build(
                getApplication(), s.profile, s.nick,
                cameraEnabled = cs.enabled,
                rageRounds = rageRounds,
                notes = notes.toList(),
            )
            val result = client.chat(s.baseUrl, s.apiKey, s.model, system, history.toList(), img)
            val actions = result.actions.map { parseAction(it) }
            val (executed, dropped) = safety.apply(actions)
            // M3：双通道保底——记录每通道最近调整轮次并自动修复
            turnCount++
            for (a in actions) {
                when (a.op) {
                    "hold_strength", "temp_strength", "add_strength" -> a.channel?.let { lastStrength[it] = turnCount }
                    "pulse_hold", "pulse" -> a.channel?.let { lastWave[it] = turnCount }
                }
            }
            applyChannelFloor()
            val note = buildString {
                for (e in executed) append("▶ ").append(e.label).append("\n")
                for (d in dropped) append("✖ ").append(d.reason).append("\n")
            }.trimEnd()
            _messages.value = _messages.value + UiMsg("ai", result.line, note)
            if (userText != null) history += (userText to result.line)
            else history += ("（自动运行）" to result.line)
            if (history.size > 20) history.removeAt(0)
        } catch (e: Exception) {
            _messages.value = _messages.value + UiMsg("ai", "模型走神了……再发一次？", e.message ?: "")
        } finally {
            _busy.value = false
        }
    }

    /**
     * M3：双通道保底（复刻桌面 _apply_channel_floor）：
     * 第 2 轮起两通道都必须有波形 + 非零强度（自动修复）；每 2 轮内强度与波形至少各调整一次。
     */
    private suspend fun applyChannelFloor() {
        if (deviceState.value.status != "connected") return
        for (ch in listOf("A", "B")) {
            val base = if (ch == "A") 15 else 5
            val waveActive = ble.waveActive(ch)
            val strength = safety.strengths.value[ch] ?: 0
            var fixed = false
            if (turnCount >= 2) {
                if (!waveActive) {
                    safety.apply(listOf(DeviceAction("pulse_hold", ch, null, DEFAULT_WAVE, null)))
                    lastWave[ch] = turnCount
                    fixed = true
                }
                if (strength <= 0) {
                    safety.apply(listOf(DeviceAction("hold_strength", ch, base, null, null)))
                    lastStrength[ch] = turnCount
                    fixed = true
                }
            }
            if (turnCount - (lastStrength[ch] ?: 0) >= 2) {
                val delta = if (strength < 100) 5 else -5
                safety.apply(listOf(DeviceAction("add_strength", ch, delta, null, null)))
                lastStrength[ch] = turnCount
                fixed = true
            }
            if (turnCount - (lastWave[ch] ?: 0) >= 2) {
                safety.apply(listOf(DeviceAction("pulse_hold", ch, null, DEFAULT_WAVE, null)))
                lastWave[ch] = turnCount
                fixed = true
            }
            if (fixed) Log.i(TAG, "通道保底：$ch 已自动补齐（第 $turnCount 轮）")
        }
    }

    private fun restartLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (true) {
                val s = _settings.value
                if (s.autopilot && s.apiKey.isNotBlank() && !_busy.value) {
                    turn()
                }
                delay(12_000L)
            }
        }
    }

    /** 急停不做；复位供断开场景调用。 */
    fun resetDevice() {
        viewModelScope.launch { safety.reset() }
    }

    companion object {
        private const val TAG = "MainVM"
        private const val DEFAULT_WAVE = "呼吸"
    }
}
