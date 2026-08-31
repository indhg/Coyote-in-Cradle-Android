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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.indhg.aiforcoyote.MainViewModel
import com.indhg.aiforcoyote.R
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
    else -> R.drawable.theme_cushou
}

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val dlcRefresh by vm.dlcRefresh.collectAsState()
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
            Text("设置", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("返回", color = Muted) }
        }

        Text("AI 模型配置", fontSize = 13.sp, color = Muted)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key", fontSize = 12.sp) },
            placeholder = {
                Text(
                    if (settings.apiKey.isNotBlank()) "已保存（留空则保持不变）" else "粘贴你的 API Key",
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
            label = { Text("模型名", fontSize = 12.sp) },
            colors = inputColors,
        )
        // JSON 模式开关：中转站不支持 json_object 时关闭（程序有兜底解析，400 也会自动降级重试）
        var jsonMode by remember { mutableStateOf(settings.jsonMode) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("JSON 模式", fontSize = 13.sp, color = Muted)
            Spacer(Modifier.width(8.dp))
            Text(
                "部分中转站不兼容，可关闭",
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
            ) { Text("测试连接", fontSize = 13.sp, color = Muted) }
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
                    status = "已保存（立即生效）" to true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        }
        status?.let { (msg, ok) ->
            Text(msg, fontSize = 12.sp, color = if (ok) Gold else Color(0xFFE06C5A))
        }

        Spacer(Modifier.height(4.dp))
        Text("配对郊狼", fontSize = 13.sp, color = Muted)
        DeviceSection(vm)

        Text("主题设置", fontSize = 13.sp, color = Muted)
        ThemeCard(
            vm = vm,
            role = settings.role,
            profile = settings.profile,
            onImport = { pickLauncher.launch(Unit) },
        )

        Text("称谓（AI 怎么叫你）", fontSize = 13.sp, color = Muted)
        OutlinedTextField(
            value = nick,
            onValueChange = { nick = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("称谓", fontSize = 12.sp) },
            colors = inputColors,
        )
        TextButton(
            onClick = {
                vm.updateSettings { it.copy(nick = nick.trim().ifBlank { it.nick }) }
                status = "昵称已保存" to true
            },
        ) { Text("保存昵称", fontSize = 13.sp, color = Muted) }

        Text("观察开关", fontSize = 13.sp, color = Muted)
        SensorSection(vm)

        Text("强度上限（默认 100，郊狼满值 200）", fontSize = 13.sp, color = Muted)
        CapSection(vm)

        Text(
            "仅供成年人、双方自愿的虚构角色扮演使用。心脏病、心脏起搏器等健康风险人群请勿使用。",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = Faint,
        )
        Text(
            "Coyote in Cradle v${com.indhg.aiforcoyote.BuildConfig.VERSION_NAME}（build ${com.indhg.aiforcoyote.BuildConfig.VERSION_CODE}）",
            fontSize = 11.sp,
            color = Faint,
        )
    }
}

/** 主题卡：只显示当前主题一行；点开浮层——上段主题列表、下段三档（支持的才亮）、底部导入。 */
@Composable
private fun ThemeCard(
    vm: MainViewModel,
    role: String,
    profile: String,
    onImport: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var roleListOpen by remember { mutableStateOf(false) }
    val current = Roles.find(role) ?: Roles.ALL.first()
    val curProfile = current.profiles.firstOrNull { it.name == profile } ?: current.profiles.first()
    val (lvColor, lvBg) = levelColors(curProfile.level)

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
                    Text(current.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${Roles.LEVEL_LABELS[curProfile.level] ?: curProfile.level}",
                        modifier = Modifier
                            .background(lvBg, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        fontSize = 11.sp,
                        color = lvColor,
                    )
                }
                Text("点击切换主题与风格", fontSize = 11.sp, color = Faint)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; roleListOpen = false },
            properties = PopupProperties(focusable = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.width(280.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("主题", fontSize = 11.sp, color = Muted)
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
                        Text(current.name, fontSize = 13.sp, color = TextMain, modifier = Modifier.weight(1f))
                        Text("（点击换主题）", fontSize = 10.sp, color = Faint)
                    }
                } else {
                    Roles.ALL.forEach { r ->
                        val usable = vm.roleUsable(r.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = usable) {
                                    val firstAvail = r.profiles.firstOrNull { vm.profileAvailable(r.name, it.name) }
                                        ?: r.profiles.first()
                                    vm.setRoleProfile(r.name, firstAvail.name)
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
                            Text(
                                r.name + if (!usable) "（未装）" else "",
                                fontSize = 13.sp,
                                color = if (usable) TextMain else Faint,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text("风格（支持档才亮）", fontSize = 11.sp, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Roles.LEVELS.forEach { lv ->
                        val p = current.profiles.firstOrNull { it.level == lv }
                        val lit = p != null && vm.profileAvailable(current.name, p.name)
                        val selected = p?.name == profile
                        val (c, bg) = levelColors(lv)
                        Button(
                            onClick = { p?.let { vm.setRoleProfile(current.name, it.name) } },
                            enabled = lit,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Gold else bg,
                                contentColor = if (selected) Ink else if (lit) c else Faint,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Faint,
                            ),
                        ) {
                            Text(
                                "${Roles.LEVEL_LABELS[lv] ?: lv}${if (!lit) "" else ""}",
                                fontSize = 12.sp,
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
                ) { Text("导入 DLC（打开文件管理）", fontSize = 12.sp, color = Muted) }
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
        label = "摄像头",
        on = camSwitch,
        err = if (camSwitch && !camState.enabled && camState.error.isNotEmpty()) camState.error else "",
        onChange = { vm.setCamSwitch(it) },
    )
    SensorRow(
        label = "麦克风",
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
            Text("$ch 通道", fontSize = 14.sp, color = TextMain, modifier = Modifier.width(56.dp))
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
        "connected" -> "已连接" + (device.battery?.let { " · 电量 $it%" } ?: "")
        "scanning" -> "扫描中…（郊狼需开机且靠近手机）"
        "connecting" -> "连接中…"
        else -> "未连接"
    }
    val scanDevices by vm.scanDevices.collectAsState()
    Text("郊狼设备", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
    Spacer(Modifier.height(6.dp))
    Text(statusText, fontSize = 12.sp, color = if (device.status == "connected") Gold else Muted)
    if (device.error.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(device.error, fontSize = 11.sp, lineHeight = 15.sp, color = Faint)
    }
    if (device.status != "connected") {
        Spacer(Modifier.height(6.dp))
        Text(
            "连接步骤：① 郊狼开机并靠近手机 ② 点「连接郊狼」 ③ 扫描列表里点信号最强的设备\n请不要在官方 App 已连接郊狼的情况下使用。",
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = Faint,
        )
    }
    if (device.status == "scanning" && scanDevices.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text("点选设备直连（信号强的在前）：", fontSize = 11.sp, color = Faint)
        scanDevices.take(8).forEach { d ->
            TextButton(onClick = { vm.connectToDeviceByAddr(d.address) }) {
                Text(
                    "${d.name ?: "无名设备"}  ${d.address}  ${d.rssi}dBm",
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    if (device.status == "connected") {
        OutlinedButton(onClick = { vm.disconnectDevice() }) { Text("断开", fontSize = 13.sp, color = Muted) }
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
        ) { Text("连接郊狼", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
    Spacer(Modifier.height(4.dp))
    Text("蓝牙直连脉冲主机，无需郊狼 App 与中继。", fontSize = 11.sp, color = Faint)
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
