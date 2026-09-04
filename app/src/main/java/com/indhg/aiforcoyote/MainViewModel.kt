package com.indhg.aiforcoyote

import android.app.Application
import android.net.Uri
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
import com.indhg.aiforcoyote.game.ScanDevice
import com.indhg.aiforcoyote.game.parseAction
import com.indhg.aiforcoyote.llm.DeepSeekClient
import com.indhg.aiforcoyote.llm.Roles
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

    private fun s(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)


    private val repo = SettingsRepository(app)
    private val client = DeepSeekClient()

    private var safetyRef: Safety? = null
    private val ble = BleCoyote(app, viewModelScope, onDisconnect = { viewModelScope.launch { safetyRef?.reset() } })
    private val safety = Safety(ble, app)

    val deviceState: StateFlow<DeviceState> = ble.state
    val scanDevices: StateFlow<List<ScanDevice>> = ble.scanDevices
    val caps: StateFlow<Map<String, Int>> = safety.caps

    // M2：观察闭环（摄像头截帧 + 麦克风音量分级）+ 独立开关
    private val camera = CameraObserver(app)
    private val audio = AudioObserver(app, viewModelScope, onMoan = { kind, _ -> addNote(moanText(kind)) })
    val cameraState: StateFlow<CameraState> = camera.state
    val audioState: StateFlow<AudioState> = audio.state
    private val _camSwitch = MutableStateFlow(true)
    val camSwitch: StateFlow<Boolean> = _camSwitch.asStateFlow()
    private val _micSwitch = MutableStateFlow(true)
    val micSwitch: StateFlow<Boolean> = _micSwitch.asStateFlow()
    private var observing = false
    private var lastOwner: LifecycleOwner? = null
    private val notes = mutableListOf<String>()
    private var rageRounds = 0
    private val _rage = MutableStateFlow(0)
    val rage: StateFlow<Int> = _rage.asStateFlow()

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

    // DLC：主题/风格档可用性（导入后写 filesDir/dlc/；dlcRefresh 供 UI 触发重算）
    private val _dlcRefresh = MutableStateFlow(0)
    val dlcRefresh: StateFlow<Int> = _dlcRefresh.asStateFlow()

    // 更新检测：静默查 GitHub 最新版本（失败为空结果，不打扰）
    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    /** 检查更新（开关开着才查）；进入设置页时由 UI 调用。 */
    fun refreshUpdate() {
        if (!_settings.value.checkUpdate) return
        viewModelScope.launch {
            _updateInfo.value = UpdateChecker.check(BuildConfig.VERSION_NAME)
        }
    }

    /** 入口可用性：体验版恒可用；正式角色看 content/roles 对应语言稿是否存在。 */
    fun roleUsable(role: String): Boolean {
        val app = getApplication<Application>()
        val lang = _settings.value.contentLang
        return Roles.find(role)?.available(app, lang) == true
    }

    private val history = mutableListOf<Pair<String, String>>()
    private var loopJob: Job? = null

    init {
        viewModelScope.launch {
            repo.settings.collect { s ->
                val autopilotChanged = s.autopilot != _settings.value.autopilot
                _settings.value = s
                safety.setIntensityLevel(s.intensityLevel)
                if (autopilotChanged) restartLoop()
            }
        }
        viewModelScope.launch { restartLoop() }
        // 启动 5 秒后静默查一次更新（开关开着才生效）
        viewModelScope.launch {
            delay(5000)
            refreshUpdate()
        }

        // BLE 直连郊狼（断开时安全层自动清零）
        safetyRef = safety
    }

    fun connectDevice() {
        ble.connect()
    }

    fun connectToDeviceByAddr(addr: String) {
        ble.connectTo(addr)
    }

    fun disconnectDevice() {
        ble.disconnect()
    }

    /** 前台开始观察（摄像头 + 麦克风，各自受开关控制）；权限缺失的自动降级不崩溃。 */
    fun startObservation(owner: LifecycleOwner) {
        observing = true
        lastOwner = owner
        if (_camSwitch.value) camera.start(owner)
        if (_micSwitch.value) audio.start()
    }

    /** 摄像头独立开关（观察进行中时立即生效）。 */
    fun setCamSwitch(on: Boolean) {
        _camSwitch.value = on
        if (!observing) return
        val owner = lastOwner ?: return
        if (on) camera.start(owner) else camera.stop()
    }

    /** 麦克风独立开关（观察进行中时立即生效）。 */
    fun setMicSwitch(on: Boolean) {
        _micSwitch.value = on
        if (!observing) return
        if (on) audio.start() else audio.stop()
    }

    /** 调通道强度上限（1~200，不持久化）。 */
    fun setChannelCap(ch: String, value: Int) {
        safety.setUserCap(ch, value)
    }

    /** 切换角色入口（未导入拦截 + 提示）。点过任意入口后体验版「新手推荐」角标消失。 */
    fun setRole(role: String) {
        if (!roleUsable(role)) {
            _toast.value = s(R.string.toast_role_missing, UiLabels.role(getApplication(), role))
            return
        }
        updateSettings { it.copy(role = role, trialBadgeSeen = true) }
    }

    /** 电击强度档（轻 ×0.7 / 中 ×1.0 / 重 ×1.3），只乘 AI 输出强度。 */
    fun setIntensityLevel(level: String) {
        if (level !in Roles.INTENSITY_LEVELS) return
        updateSettings { it.copy(intensityLevel = level) }
    }

    /** 角色稿语言（与界面语言独立）。手动指定后不再跟随界面。 */
    fun setContentLang(lang: String) {
        if (lang != Roles.LANG_ZH && lang != Roles.LANG_EN) return
        val app = getApplication<Application>()
        val ui = LocalePrefs.resolved(app)
        updateSettings { cur ->
            val next = cur.copy(contentLang = lang, scriptFollowUi = false)
            val still = Roles.find(cur.role)?.available(app, lang) == true
            val adjusted = if (still) next else next.copy(role = "体验版")
            adjusted
        }
        if (ui == Roles.LANG_ZH && lang == Roles.LANG_EN) {
            _toast.value = s(R.string.trial_script_mismatch)
        }
        _dlcRefresh.value++
    }

    /** 界面语言：system / zh / en。默认跟随系统；切换后 AppCompat 重建 Activity。
     * 必须先等 DataStore 写入完成再 setApplicationLocales，否则重建时 collect 到旧 uiLang/contentLang，
     * 表现为按钮高亮/角色稿语言与界面语言不一致（中英切换异常）。 */
    fun setUiLang(tag: String) {
        val v = if (tag == LocalePrefs.ZH || tag == LocalePrefs.EN) tag else LocalePrefs.SYSTEM
        val app = getApplication<Application>()
        LocalePrefs.set(app, v)
        val resolved = LocalePrefs.resolved(app)
        viewModelScope.launch {
            repo.update { cur ->
                var next = cur.copy(uiLang = v)
                if (cur.scriptFollowUi) {
                    next = next.copy(contentLang = resolved)
                    val still = Roles.find(next.role)?.available(app, next.contentLang) == true
                    if (!still) next = next.copy(role = "体验版")
                }
                next
            }
            LocalePrefs.apply(v)
        }
    }

    /** 调教版 DLC：导入单个文件（zip 解出全部 .md / 单 md 按真实文件名）。分享入口用。 */
    fun importDlc(uri: Uri): Boolean = importDlcUris(listOf(uri))

    /** 多选导入：逐个导入并汇总结果（失败项在 toast 中列出）。 */
    fun importDlcUris(uris: List<Uri>): Boolean {
        if (uris.isEmpty()) return false
        val app = getApplication<Application>()
        val lang = _settings.value.contentLang
        val before = Roles.ALL.filter { it.available(app, lang) }.map { it.name }.toSet()
        val failures = mutableListOf<String>()
        for (uri in uris) {
            val display = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: s(R.string.toast_unknown_file)
            val reason = try {
                if (display.endsWith(".zip", ignoreCase = true)) importDlcZip(app, uri)
                else importDlcMd(app, uri)
            } catch (e: Exception) {
                s(R.string.toast_exception, e.message ?: "")
            }
            if (reason != null) failures += "$display：$reason"
        }
        _dlcRefresh.value++
        val after = Roles.ALL.filter { it.available(app, lang) }.map { it.name }.toSet()
        val newRoles = (after - before).toList()
        _toast.value = when {
            failures.isNotEmpty() -> s(R.string.toast_import_fail, failures.size, failures.joinToString("；"))
            newRoles.isNotEmpty() -> s(R.string.toast_import_new, newRoles.joinToString("、") { UiLabels.role(app, it) })
            else -> s(R.string.toast_import_ok)
        }
        return failures.isEmpty()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * zip 包：只收下现行 DLC 文件名（`触手-角色提示词.md` / `…-EN.md`），
     * 写入 filesDir/content/roles/。拒绝 `..` / 绝对路径；忽略旧 -调教/-凌辱 文件。
     */
    private fun importDlcZip(app: Application, uri: Uri): String? {
        val dir = Roles.contentRolesDir(app)
        dir.mkdirs()
        var accepted = 0
        var skippedLegacy = 0
        app.contentResolver.openInputStream(uri)?.use { ins ->
            java.util.zip.ZipInputStream(ins).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val raw = entry.name.replace('\\', '/')
                    val name = raw.substringAfterLast('/')
                    val unsafe = raw.contains("..") || raw.startsWith("/") || name.isBlank()
                    if (!entry.isDirectory && !unsafe && name.endsWith(".md", ignoreCase = true)) {
                        when {
                            name in Roles.KNOWN_DLC_FILES -> {
                                java.io.File(dir, name).outputStream().use { out -> zip.copyTo(out) }
                                accepted++
                            }
                            name.contains("调教") || name.contains("凌辱") || name.contains("正式") ->
                                skippedLegacy++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return s(R.string.err_cant_read)
        return when {
            accepted > 0 -> null
            skippedLegacy > 0 -> s(R.string.err_legacy_pack)
            else -> s(R.string.err_no_role_md)
        }
    }

    /** 单 md：仅接受现行文件名，落入 content/roles。 */
    private fun importDlcMd(app: Application, uri: Uri): String? {
        val name = (queryDisplayName(uri) ?: uri.lastPathSegment)?.substringAfterLast('/') ?: return s(R.string.err_cant_read_name)
        if (name !in Roles.KNOWN_DLC_FILES) {
            return s(R.string.err_unknown_md)
        }
        val dir = Roles.contentRolesDir(app)
        dir.mkdirs()
        val target = java.io.File(dir, name)
        app.contentResolver.openInputStream(uri)?.use { ins ->
            target.outputStream().use { out -> ins.copyTo(out) }
        } ?: return s(R.string.err_cant_read)
        return null
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

    private fun moanText(kind: String): String {
        val en = _settings.value.contentLang == Roles.LANG_EN
        return if (kind == "high") {
            if (en) s(R.string.moan_high_en) else s(R.string.moan_high)
        } else {
            if (en) s(R.string.moan_mid_en) else s(R.string.moan_mid)
        }
    }

    fun clearToast() {
        _toast.value = null
    }

    /** 清空对话历史（界面消息 + 模型上下文）。 */
    fun clearHistory() {
        _messages.value = emptyList()
        history.clear()
        _toast.value = s(R.string.toast_cleared)
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
            r.onSuccess { onDone(s(R.string.conn_ok), true) }
            r.onFailure { e ->
                val msg = when (e.message) {
                    "401" -> s(R.string.err_api_key)
                    "400" -> s(R.string.err_http_400)
                    else -> e.message ?: s(R.string.conn_fail)
                }
                onDone(msg, false)
            }
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
                _toast.value = s(R.string.toast_need_api)
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
            _rage.value = rageRounds
            val img = if (cs.enabled && cs.hasFrame) camera.base64() else null
            if (!roleUsable(s.role)) {
                _toast.value = s(R.string.toast_role_fallback, UiLabels.role(getApplication(), s.role))
                updateSettings { it.copy(role = "体验版") }
                _busy.value = false
                return
            }
            val system = SystemPrompt.build(
                getApplication(), s.role, s.nick,
                lang = s.contentLang,
                uiLang = LocalePrefs.resolved(getApplication()),
                cameraEnabled = cs.enabled,
                rageRounds = rageRounds,
                notes = notes.toList(),
            )
            val result = client.chat(
                s.baseUrl, s.apiKey, s.model, system, history.toList(), img,
                jsonMode = s.jsonMode,
            )
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
            else history += (s(R.string.autopilot_turn) to result.line)
            if (history.size > 20) history.removeAt(0)
        } catch (e: Exception) {
            _messages.value = _messages.value + UiMsg("ai", s(R.string.toast_model_blank), e.message ?: "")
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
                    safety.apply(listOf(DeviceAction("pulse_hold", ch, null, DEFAULT_WAVE, null)), applyScale = false)
                    lastWave[ch] = turnCount
                    fixed = true
                }
                if (strength <= 0) {
                    safety.apply(listOf(DeviceAction("hold_strength", ch, base, null, null)), applyScale = false)
                    lastStrength[ch] = turnCount
                    fixed = true
                }
            }
            if (turnCount - (lastStrength[ch] ?: 0) >= 2) {
                val delta = if (strength < safety.capFor(ch)) 5 else -5
                safety.apply(listOf(DeviceAction("add_strength", ch, delta, null, null)), applyScale = false)
                lastStrength[ch] = turnCount
                fixed = true
            }
            if (turnCount - (lastWave[ch] ?: 0) >= 2) {
                safety.apply(listOf(DeviceAction("pulse_hold", ch, null, DEFAULT_WAVE, null)), applyScale = false)
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
