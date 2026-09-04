package com.indhg.aiforcoyote.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.indhg.aiforcoyote.LocalePrefs
import com.indhg.aiforcoyote.MainViewModel
import com.indhg.aiforcoyote.R
import com.indhg.aiforcoyote.UiLabels
import com.indhg.aiforcoyote.llm.Roles
import com.indhg.aiforcoyote.ui.theme.Faint
import com.indhg.aiforcoyote.ui.theme.Gold
import com.indhg.aiforcoyote.ui.theme.Ink
import com.indhg.aiforcoyote.ui.theme.Line
import com.indhg.aiforcoyote.ui.theme.Muted
import com.indhg.aiforcoyote.ui.theme.TextMain

private val LevelGreen = Color(0xFF4ADE80)
private val LevelRed = Color(0xFFF87171)
private val WarnYellow = Color(0xFFFFC966)

private fun levelColors(level: String): Pair<Color, Color> = when (level) {
    "轻" -> LevelGreen to Color(0x334ADE80)
    "重" -> LevelRed to Color(0x33F87171)
    else -> Gold to Color(0x33F7D97A)
}

private fun roleAvatarRes(role: String): Int = when (role) {
    "品评会" -> R.drawable.theme_pingpinghui
    "哥布林" -> R.drawable.theme_gebulin
    "史莱姆" -> R.drawable.theme_shilaimu
    "蛛后" -> R.drawable.theme_zhuhou
    else -> R.drawable.theme_cushou // 体验版 / 触手
}

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val dlcRefresh by vm.dlcRefresh.collectAsState()
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var model by remember { mutableStateOf(settings.model) }
    var nick by remember { mutableStateOf(settings.nick) }
    var status by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    // 调起系统文件管理选文件（vivo/小米等各自文件管理的分类页），多选 zip/md
    val pickLauncher = rememberLauncherForActivityResult(DlcPickContract()) { uris ->
        if (uris.isNotEmpty()) vm.importDlcUris(uris)
    }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Gold,
        unfocusedBorderColor = Line,
        focusedTextColor = TextMain,
        unfocusedTextColor = TextMain,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = Muted) }
        }

        Text(stringResource(R.string.ui_language), fontSize = 13.sp, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                LocalePrefs.SYSTEM to stringResource(R.string.lang_system),
                LocalePrefs.ZH to stringResource(R.string.lang_zh),
                LocalePrefs.EN to stringResource(R.string.lang_en),
            ).forEach { (code, label) ->
                // 以 LocalePrefs 为准：DataStore 异步写入时 settings.uiLang 可能短暂滞后
                val selected = LocalePrefs.get(context) == code
                OutlinedButton(
                    onClick = { vm.setUiLang(code) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, fontSize = 12.sp, color = if (selected) Gold else Muted)
                }
            }
        }

        Text(stringResource(R.string.ai_model), fontSize = 13.sp, color = Muted)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key", fontSize = 12.sp) },
            placeholder = {
                Text(
                    if (settings.apiKey.isNotBlank()) stringResource(R.string.api_key_saved) else stringResource(R.string.api_key_paste),
                    fontSize = 12.sp,
                    color = Faint,
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            colors = inputColors,
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Base URL", fontSize = 12.sp) },
            colors = inputColors,
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.model_name), fontSize = 12.sp) },
            colors = inputColors,
        )
        // JSON 模式开关：中转站不支持 json_object 时关闭（程序有兜底解析，400 也会自动降级重试）
        var jsonMode by remember { mutableStateOf(settings.jsonMode) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.json_mode), fontSize = 13.sp, color = Muted)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.json_mode_hint),
                fontSize = 10.sp,
                color = Faint,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = jsonMode,
                onCheckedChange = { jsonMode = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Gold),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    status = null
                    vm.testConnection(apiKey.ifBlank { settings.apiKey }, baseUrl, model) { msg, ok ->
                        status = msg to ok
                    }
                },
            ) { Text(stringResource(R.string.test_connection), fontSize = 13.sp, color = Muted) }
            Button(
                onClick = {
                    val key = apiKey.ifBlank { settings.apiKey }
                    vm.updateSettings {
                        it.copy(
                            apiKey = key,
                            baseUrl = baseUrl.ifBlank { it.baseUrl },
                            model = model.ifBlank { it.model },
                            jsonMode = jsonMode,
                        )
                    }
                    apiKey = ""
                    status = context.getString(R.string.saved_now) to true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        }
        status?.let { (msg, ok) ->
            Text(msg, fontSize = 12.sp, color = if (ok) Gold else Color(0xFFE06C5A))
        }

        // 更新检测：进设置页查一次 + 开关 + 发现新版本跳 GitHub
        val updateInfo by vm.updateInfo.collectAsState()
        LaunchedEffect(Unit) { vm.refreshUpdate() }
        Text(stringResource(R.string.update), fontSize = 13.sp, color = Muted)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (updateInfo.available && updateInfo.url.isNotBlank()) {
                    Text(
                        stringResource(R.string.update_available, updateInfo.latest),
                        fontSize = 12.sp,
                        color = Gold,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.url)))
                        },
                    )
                } else {
                    Text(
                        if (updateInfo.latest.isNotBlank()) stringResource(R.string.update_latest, updateInfo.latest) else stringResource(R.string.update_none),
                        fontSize = 12.sp,
                        color = Faint,
                    )
                }
            }
            Text(stringResource(R.string.auto_check), fontSize = 11.sp, color = Muted)
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = settings.checkUpdate,
                onCheckedChange = { on ->
                    vm.updateSettings { it.copy(checkUpdate = on) }
                    if (on) vm.refreshUpdate()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Gold),
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.pair_coyote), fontSize = 13.sp, color = Muted)
        DeviceSection(vm)

        Text(stringResource(R.string.role_entry), fontSize = 13.sp, color = Muted)
        ThemeCard(
            vm = vm,
            role = settings.role,
            intensityLevel = settings.intensityLevel,
            contentLang = settings.contentLang,
            scriptFollowUi = settings.scriptFollowUi,
            trialBadgeSeen = settings.trialBadgeSeen,
            dlcRefresh = dlcRefresh,
            onImport = { pickLauncher.launch(Unit) },
        )

        Text(stringResource(R.string.nick_section), fontSize = 13.sp, color = Muted)
        OutlinedTextField(
            value = nick,
            onValueChange = { nick = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.nick_label), fontSize = 12.sp) },
            colors = inputColors,
        )
        TextButton(
            onClick = {
                vm.updateSettings { it.copy(nick = nick.trim().ifBlank { it.nick }) }
                status = context.getString(R.string.nick_saved) to true
            },
        ) { Text(stringResource(R.string.save_nick), fontSize = 13.sp, color = Muted) }

        Text(stringResource(R.string.observe_section), fontSize = 13.sp, color = Muted)
        SensorSection(vm)

        Text(stringResource(R.string.cap_section), fontSize = 13.sp, color = Muted)
        CapSection(vm)

        Text(
            stringResource(R.string.disclaimer),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = Faint,
        )
        Text(
            stringResource(
                R.string.version_line,
                com.indhg.aiforcoyote.BuildConfig.VERSION_NAME,
                com.indhg.aiforcoyote.BuildConfig.VERSION_CODE,
            ),
            fontSize = 11.sp,
            color = Faint,
        )
    }
}

/** 入口卡：当前角色一行；点开浮层——入口列表（未导入灰禁+角标）、电击强度三档、语言、导入。 */
@Composable
private fun ThemeCard(
    vm: MainViewModel,
    role: String,
    intensityLevel: String,
    contentLang: String,
    scriptFollowUi: Boolean,
    trialBadgeSeen: Boolean,
    dlcRefresh: Int,
    onImport: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var roleListOpen by remember { mutableStateOf(false) }
    val current = Roles.find(role) ?: Roles.ALL.first()
    val (lvColor, lvBg) = levelColors(intensityLevel)
    val ctx = LocalContext.current
    // 读取 dlcRefresh：导入成功后父级递增，本卡重组并重扫 content/roles
    @Suppress("UNUSED_VARIABLE")
    val importGen = dlcRefresh

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .border(1.dp, Line, RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(roleAvatarRes(current.name)),
                contentDescription = current.name,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Line, RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(UiLabels.role(ctx, current.name), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.intensity_with_level, UiLabels.intensity(ctx, intensityLevel)),
                        modifier = Modifier
                            .background(lvBg, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        fontSize = 11.sp,
                        color = lvColor,
                    )
                }
                Text(stringResource(R.string.tap_switch_role_intensity), fontSize = 11.sp, color = Faint)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; roleListOpen = false },
            properties = PopupProperties(focusable = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.width(280.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.roles), fontSize = 11.sp, color = Muted)
                if (!roleListOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { roleListOpen = true }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(roleAvatarRes(current.name)),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(UiLabels.role(ctx, current.name), fontSize = 13.sp, color = TextMain, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.tap_change_role), fontSize = 10.sp, color = Faint)
                    }
                } else {
                    Roles.ALL.forEach { r ->
                        val usable = vm.roleUsable(r.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = usable) {
                                    vm.setRole(r.name)
                                    roleListOpen = false
                                }
                                .background(if (r.name == role) Color(0x22F7D97A) else Color.Transparent, RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(roleAvatarRes(r.name)),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(if (usable) Color.Transparent else Color(0x33000000), RoundedCornerShape(4.dp)),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    UiLabels.role(ctx, r.name),
                                    fontSize = 13.sp,
                                    color = if (usable) TextMain else Faint,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (r.trial && !trialBadgeSeen) {
                                        Text(stringResource(R.string.badge_start_here), fontSize = 10.sp, color = Gold)
                                    }
                                    if (r.recommended && usable) {
                                        Text(stringResource(R.string.badge_pick), fontSize = 10.sp, color = Gold)
                                    }
                                    if (!usable) {
                                        Text(stringResource(R.string.badge_not_installed), fontSize = 10.sp, color = Faint)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.shock_intensity), fontSize = 11.sp, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Roles.INTENSITY_LEVELS.forEach { lv ->
                        val selected = lv == intensityLevel
                        val (c, bg) = levelColors(lv)
                        Button(
                            onClick = { vm.setIntensityLevel(lv) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Gold else bg,
                                contentColor = if (selected) Ink else c,
                            ),
                        ) {
                            Text(UiLabels.intensity(ctx, lv), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.script_language), fontSize = 11.sp, color = Muted)
                Text(stringResource(R.string.script_language_note), fontSize = 10.sp, lineHeight = 14.sp, color = Faint)
                if (scriptFollowUi) {
                    Text(stringResource(R.string.script_follow_hint), fontSize = 10.sp, color = Gold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Roles.LANG_ZH to stringResource(R.string.lang_zh), Roles.LANG_EN to stringResource(R.string.lang_en)).forEach { (code, label) ->
                        val selected = contentLang == code
                        OutlinedButton(
                            onClick = { vm.setContentLang(code) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = if (selected) Gold else Muted,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                OutlinedButton(
                    onClick = {
                        expanded = false
                        onImport()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.import_dlc), fontSize = 12.sp, color = Muted) }
                Text(
                    stringResource(R.string.import_dlc_hint),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = Faint,
                )
            }
        }
    }
}

/** 观察开关：摄像头/麦克风独立开关 + 失败警示。 */
@Composable
private fun SensorSection(vm: MainViewModel) {
    val camSwitch by vm.camSwitch.collectAsState()
    val micSwitch by vm.micSwitch.collectAsState()
    val camState by vm.cameraState.collectAsState()
    val audioState by vm.audioState.collectAsState()

    SensorRow(
        label = stringResource(R.string.camera),
        on = camSwitch,
        err = if (camSwitch && !camState.enabled && camState.error.isNotEmpty()) camState.error else "",
        onChange = { vm.setCamSwitch(it) },
    )
    SensorRow(
        label = stringResource(R.string.mic),
        on = micSwitch,
        err = if (micSwitch && !audioState.running && audioState.error.isNotEmpty()) audioState.error else "",
        onChange = { vm.setMicSwitch(it) },
    )
}

@Composable
private fun SensorRow(label: String, on: Boolean, err: String, onChange: (Boolean) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, color = TextMain, modifier = Modifier.weight(1f))
            if (err.isNotEmpty()) {
                Text("⚠", fontSize = 12.sp, color = WarnYellow)
                Spacer(Modifier.width(6.dp))
            }
            Switch(
                checked = on,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink,
                    checkedTrackColor = if (err.isEmpty()) LevelGreen else WarnYellow,
                    uncheckedTrackColor = Line,
                ),
            )
        }
        if (err.isNotEmpty()) {
            Text(err, fontSize = 11.sp, lineHeight = 15.sp, color = WarnYellow)
        }
    }
}

/** 通道强度上限：1~200 滑杆，默认 100。 */
@Composable
private fun CapSection(vm: MainViewModel) {
    val caps by vm.caps.collectAsState()
    listOf("A", "B").forEach { ch ->
        val cap = caps[ch] ?: 100
        var draft by remember(cap) { mutableStateOf(cap) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.channel_n, ch), fontSize = 14.sp, color = TextMain, modifier = Modifier.width(72.dp))
            Slider(
                value = draft.toFloat(),
                onValueChange = { draft = it.toInt() },
                onValueChangeFinished = { vm.setChannelCap(ch, draft) },
                valueRange = 1f..200f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Line),
            )
            Text("$draft", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold, modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
private fun DeviceSection(vm: MainViewModel) {
    val device by vm.deviceState.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) vm.connectDevice()
    }
    val statusText = when (device.status) {
        "connected" -> device.battery?.let { stringResource(R.string.dev_connected_bat, it) }
            ?: stringResource(R.string.dev_connected)
        "scanning" -> stringResource(R.string.dev_scanning)
        "connecting" -> stringResource(R.string.dev_connecting)
        else -> stringResource(R.string.dev_disconnected)
    }
    val scanDevices by vm.scanDevices.collectAsState()
    Text(stringResource(R.string.coyote_device), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
    Spacer(Modifier.height(6.dp))
    Text(statusText, fontSize = 12.sp, color = if (device.status == "connected") Gold else Muted)
    if (device.error.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(device.error, fontSize = 11.sp, lineHeight = 15.sp, color = Faint)
    }
    if (device.status != "connected") {
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.dev_connect_steps),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = Faint,
        )
    }
    if (device.status == "scanning" && scanDevices.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.dev_pick_list), fontSize = 11.sp, color = Faint)
        scanDevices.take(8).forEach { d ->
            TextButton(onClick = { vm.connectToDeviceByAddr(d.address) }) {
                Text(
                    "${d.name ?: stringResource(R.string.dev_unnamed)}  ${d.address}  ${d.rssi}dBm",
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    if (device.status == "connected") {
        OutlinedButton(onClick = { vm.disconnectDevice() }) { Text(stringResource(R.string.disconnect), fontSize = 13.sp, color = Muted) }
    } else {
        Button(
            onClick = {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                val missing = perms.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) vm.connectDevice() else launcher.launch(perms)
            },
            enabled = device.status != "scanning" && device.status != "connecting",
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
        ) { Text(stringResource(R.string.connect_coyote), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

/**
 * 调起系统文件管理选文件（ACTION_GET_CONTENT）：
 * vivo/小米/华为等会弹出各自文件管理的分类页（图片/压缩包等），原生安卓弹系统文档页；支持多选。
 */
private class DlcPickContract : ActivityResultContract<Unit, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream",
                    "text/markdown",
                    "text/x-markdown",
                    "text/plain",
                ),
            )
        }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val clip = intent.clipData
        return when {
            clip != null -> (0 until clip.itemCount).map { clip.getItemAt(it).uri }
            intent.data != null -> listOf(intent.data!!)
            else -> emptyList()
        }
    }
}
