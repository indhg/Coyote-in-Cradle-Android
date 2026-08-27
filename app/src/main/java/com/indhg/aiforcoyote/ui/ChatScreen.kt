package com.indhg.aiforcoyote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indhg.aiforcoyote.MainViewModel
import com.indhg.aiforcoyote.UiMsg
import com.indhg.aiforcoyote.ui.theme.Faint
import com.indhg.aiforcoyote.ui.theme.Gold
import com.indhg.aiforcoyote.ui.theme.Ink
import com.indhg.aiforcoyote.ui.theme.Ink3
import com.indhg.aiforcoyote.ui.theme.Line
import com.indhg.aiforcoyote.ui.theme.Muted
import com.indhg.aiforcoyote.ui.theme.TextMain

@Composable
fun ChatScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    val settings by vm.settings.collectAsState()
    val strengths by vm.strengths.collectAsState()
    val device by vm.deviceState.collectAsState()
    val toast by vm.toast.collectAsState()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    var input by remember { mutableStateOf("") }

    val pairLabel = when (device.status) {
        "connected" -> "郊狼已连接" + (device.battery?.let { " $it%" } ?: "")
        "scanning" -> "扫描郊狼中…"
        "connecting" -> "连接郊狼中…"
        else -> "郊狼未连接"
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(toast) {
        toast?.let { snackbar.showSnackbar(it); vm.clearToast() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
        // 顶栏：标题 + 风格 + 设置
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI for Coyote", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    if (settings.profile == "纯爱") "纯爱版 · 温柔驯服" else "调教版 · 支配胁迫",
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
            TextButton(onClick = onOpenSettings) { Text("设置", color = Muted) }
        }

        // 状态行：A/B 强度 + 自动运行开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("A ${strengths["A"] ?: 0}", fontSize = 13.sp, color = Gold)
            Spacer(Modifier.width(12.dp))
            Text("B ${strengths["B"] ?: 0}", fontSize = 13.sp, color = Muted)
            Spacer(Modifier.width(12.dp))
            Text(
                pairLabel,
                fontSize = 12.sp,
                color = if (device.status == "connected") Gold else Muted,
            )
            Spacer(Modifier.weight(1f))
            Text("自动运行", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.width(4.dp))
            Switch(
                checked = settings.autopilot,
                onCheckedChange = { vm.toggleAutopilot() },
                colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink),
            )
        }

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { m -> Bubble(m) }
            if (busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Gold)
                        Spacer(Modifier.width(8.dp))
                        Text("触手正在观察……", fontSize = 12.sp, color = Faint)
                    }
                }
            }
        }

        // 输入行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("说点什么，或什么都不说……", fontSize = 13.sp, color = Faint) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Line,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val t = input
                    input = ""
                    vm.send(t)
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
            ) {
                Text("发送", fontWeight = FontWeight.Bold)
            }
        }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun Bubble(m: UiMsg) {
    val isUser = m.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (isUser) Ink3 else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                m.text,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = TextMain,
            )
            if (m.note.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(m.note, fontSize = 11.sp, lineHeight = 15.sp, color = Faint)
            }
        }
    }
}
