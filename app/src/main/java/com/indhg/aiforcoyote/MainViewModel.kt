package com.indhg.aiforcoyote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indhg.aiforcoyote.data.Settings
import com.indhg.aiforcoyote.data.SettingsRepository
import com.indhg.aiforcoyote.game.BleCoyote
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

    private suspend fun turn(userText: String? = null, imageB64: String? = null) {
        if (_busy.value) return
        _busy.value = true
        val s = _settings.value
        try {
            if (s.apiKey.isBlank()) {
                _toast.value = "请先在设置页填写 API Key"
                _busy.value = false
                return
            }
            val system = SystemPrompt.build(getApplication(), s.profile, s.nick)
            val result = client.chat(s.baseUrl, s.apiKey, s.model, system, history.toList(), imageB64)
            val (executed, dropped) = safety.apply(result.actions.map { parseAction(it) })
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

    private fun restartLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (true) {
                val s = _settings.value
                if (s.autopilot && s.apiKey.isNotBlank() && !_busy.value) {
                    turn(imageB64 = null)
                }
                delay(12_000L)
            }
        }
    }

    /** 急停不做；复位供断开场景调用。 */
    fun resetDevice() {
        viewModelScope.launch { safety.reset() }
    }
}
