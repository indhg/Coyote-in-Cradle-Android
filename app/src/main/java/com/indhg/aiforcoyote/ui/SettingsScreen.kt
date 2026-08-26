package com.indhg.aiforcoyote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indhg.aiforcoyote.MainViewModel
import com.indhg.aiforcoyote.qr.QrCode
import com.indhg.aiforcoyote.ui.theme.Faint
import com.indhg.aiforcoyote.ui.theme.Gold
import com.indhg.aiforcoyote.ui.theme.Ink
import com.indhg.aiforcoyote.ui.theme.Line
import com.indhg.aiforcoyote.ui.theme.Muted
import com.indhg.aiforcoyote.ui.theme.TextMain

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var model by remember { mutableStateOf(settings.model) }
    var nick by remember { mutableStateOf(settings.nick) }
    var status by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

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
                        it.copy(apiKey = key, baseUrl = baseUrl.ifBlank { it.baseUrl }, model = model.ifBlank { it.model })
                    }
                    apiKey = ""
                    status = "已保存（立即生效）" to true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        }
        status?.let { (msg, ok) ->
            Text(msg, fontSize = 12.sp, color = if (ok) Gold else androidx.compose.ui.graphics.Color(0xFFE06C5A))
        }

        Spacer(Modifier.height(4.dp))
        Text("配对郊狼", fontSize = 13.sp, color = Muted)
        PairSection(vm)

        Text("角色设置", fontSize = 13.sp, color = Muted)
        OutlinedTextField(
            value = nick,
            onValueChange = { nick = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("称谓（AI 怎么叫你）", fontSize = 12.sp) },
            colors = inputColors,
        )
        TextButton(
            onClick = {
                vm.updateSettings { it.copy(nick = nick.trim().ifBlank { it.nick }) }
                status = "昵称已保存" to true
            },
        ) { Text("保存昵称", fontSize = 13.sp, color = Muted) }

        Text("对话风格", fontSize = 13.sp, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleChip("纯爱", selected = settings.profile == "纯爱", enabled = true) {
                vm.updateSettings { it.copy(profile = "纯爱") }
            }
            StyleChip("调教", selected = settings.profile == "调教", enabled = false) {
                // DLC 未安装（M3 接入分包后启用）
            }
        }
        Text("调教版为 DLC 内容（后续版本提供下载）", fontSize = 11.sp, color = Faint)

        Text(
            "仅供成年人、双方自愿的虚构角色扮演使用。心脏病、心脏起搏器等健康风险人群请勿使用。",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = Faint,
        )
    }
}

@Composable
private fun PairSection(vm: MainViewModel) {
    val relay by vm.relayState.collectAsState()
    when (relay.status) {
        "paired" -> Text(
            "已配对 · slotId ${relay.slotId ?: "—"}",
            fontSize = 12.sp,
            color = Gold,
        )
        "waiting" -> {
            Text("用 DG-LAB 4.0 App 扫码配对（同一台手机）", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            val bmp = remember(relay.pairUrl) {
                if (relay.pairUrl.isNotEmpty()) QrCode.bitmap(relay.pairUrl) else null
            }
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "配对二维码",
                    modifier = Modifier.size(200.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(relay.pairUrl, fontSize = 10.sp, color = Faint, maxLines = 3)
        }
        "connecting" -> Text("中继连接中…", fontSize = 12.sp, color = Muted)
        else -> Text("中继未连接（杀掉应用重开重试）", fontSize = 12.sp, color = Faint)
    }
}

@Composable
private fun StyleChip(name: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Gold else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) Ink else Muted,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = Faint,
        ),
    ) {
        Text("$name${if (!enabled) "（未装DLC）" else ""}", fontSize = 13.sp)
    }
}
