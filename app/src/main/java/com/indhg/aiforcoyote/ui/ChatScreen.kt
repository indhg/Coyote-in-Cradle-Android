package com.indhg.aiforcoyote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import com.indhg.aiforcoyote.MainViewModel
import com.indhg.aiforcoyote.R
import com.indhg.aiforcoyote.UiLabels
import com.indhg.aiforcoyote.UiMsg
import com.indhg.aiforcoyote.UiMsgStatus
import com.indhg.aiforcoyote.ui.theme.Bad
import com.indhg.aiforcoyote.ui.theme.Faint
import com.indhg.aiforcoyote.ui.theme.Gold
import com.indhg.aiforcoyote.ui.theme.Ink
import com.indhg.aiforcoyote.ui.theme.Ink2
import com.indhg.aiforcoyote.ui.theme.Ink3
import com.indhg.aiforcoyote.ui.theme.Line
import com.indhg.aiforcoyote.ui.theme.Muted
import com.indhg.aiforcoyote.ui.theme.TextMain
import com.indhg.aiforcoyote.ui.theme.Warn
import com.indhg.aiforcoyote.ui.onboarding.tourTarget

@Composable
fun ChatScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    val settings by vm.settings.collectAsState()
    val strengths by vm.strengths.collectAsState()
    val device by vm.deviceState.collectAsState()
    val cameraState by vm.cameraState.collectAsState()
    val audioState by vm.audioState.collectAsState()
    val rage by vm.rage.collectAsState()
    val obsOn = cameraState.enabled || audioState.enabled
    val toast by vm.toast.collectAsState()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    var input by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    // 队列状态直接从消息流派生，避免额外的状态源与 UI 短暂不同步。
    val queuedMessages = messages.filter {
        it.role == "user" && it.status == UiMsgStatus.QUEUED
    }
    val queuePosition = queuedMessages
        .mapIndexed { index, message -> message.id to index }
        .toMap()
    val processingManual = messages.any {
        it.role == "user" && it.status == UiMsgStatus.PROCESSING
    }

    val pairLabel = when (device.status) {
        "connected" -> device.battery?.let { stringResource(R.string.chat_coyote_on_bat, it) }
            ?: stringResource(R.string.chat_coyote_on)
        "scanning" -> stringResource(R.string.chat_coyote_scan)
        "connecting" -> stringResource(R.string.chat_coyote_connecting)
        else -> stringResource(R.string.chat_coyote_off)
    }
    val ctx = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect
        // 只在用户已经接近列表底部时跟随新消息，避免回复插入旧回合时打断阅读。
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val nearBottom = lastVisible < 0 || lastVisible >= messages.size - 2
        if (nearBottom) listState.animateScrollToItem(messages.lastIndex)
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
                Text("Coyote in Cradle", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    stringResource(
                        R.string.chat_subtitle,
                        UiLabels.role(ctx, settings.role),
                        UiLabels.intensity(ctx, settings.intensityLevel),
                    ),
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
            TextButton(onClick = { showClearConfirm = true }) { Text(stringResource(R.string.clear), color = Muted) }
            TextButton(onClick = onOpenSettings, modifier = Modifier.tourTarget("settings_btn")) { Text(stringResource(R.string.settings), color = Muted) }
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
            Spacer(Modifier.width(12.dp))
            Text(
                if (obsOn) stringResource(R.string.observing_on) else stringResource(R.string.observing_off),
                fontSize = 12.sp,
                color = if (obsOn) Gold else Muted,
            )
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.autopilot), fontSize = 12.sp, color = Muted)
            Spacer(Modifier.width(4.dp))
            Switch(
                checked = settings.autopilot,
                onCheckedChange = { vm.toggleAutopilot() },
                modifier = Modifier.tourTarget("autopilot"),
                colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink),
            )
        }

        // 观察行：麦克风音量条 + 怒气值（常显；无权限/未开启时灰显提示）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (audioState.enabled) {
                Text(stringResource(R.string.mic), fontSize = 11.sp, color = Muted)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Ink3),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((audioState.levelPct / 100.0).toFloat().coerceIn(0f, 1f))
                            .background(Gold),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("${audioState.levelPct.toInt()}", fontSize = 11.sp, color = Muted)
                Spacer(Modifier.width(12.dp))
            } else {
                Text(
                    if (obsOn) stringResource(R.string.mic_off) else stringResource(R.string.observe_need_perm),
                    fontSize = 11.sp,
                    color = Faint,
                )
                Spacer(Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.rage_n, rage),
                fontSize = 12.sp,
                color = when {
                    rage >= 5 -> Bad
                    rage >= 3 -> Warn
                    rage >= 1 -> Gold
                    else -> Muted
                },
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
            items(messages, key = { it.id }) { m ->
                Bubble(
                    message = m,
                    queuedIndex = queuePosition[m.id],
                    onRetry = vm::retry,
                )
            }
        }

        // 请求轨固定在输入栏上方，不占用消息列表，也不会阻塞继续输入。
        AnimatedVisibility(
            visible = busy || queuedMessages.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(120)),
        ) {
            QueueRail(
                busy = busy,
                queuedCount = queuedMessages.size,
                processingManual = processingManual,
                onClick = { showQueueSheet = true },
            )
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
                placeholder = { Text(stringResource(R.string.input_hint), fontSize = 13.sp, color = Faint) },
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
                // 请求进行中也允许点击；ViewModel 会按 FIFO 排队。
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
            ) {
                Text(stringResource(R.string.send), fontWeight = FontWeight.Bold)
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

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(stringResource(R.string.clear_title), fontSize = 16.sp, color = TextMain) },
                text = { Text(stringResource(R.string.clear_body), fontSize = 13.sp, color = Muted) },
                confirmButton = {
                    TextButton(onClick = { showClearConfirm = false; vm.clearHistory() }) {
                        Text(stringResource(R.string.clear), color = Bad)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.cancel), color = Muted) }
                },
            )
        }

        if (showQueueSheet) {
            QueueSheet(
                queuedMessages = queuedMessages,
                onDismiss = { showQueueSheet = false },
                onCancel = vm::cancelQueued,
                onClear = {
                    vm.clearQueued()
                    showQueueSheet = false
                },
            )
        }
    }
}

@Composable
private fun Bubble(
    message: UiMsg,
    queuedIndex: Int?,
    onRetry: (Long) -> Unit,
) {
    val m = message
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
            if (isUser && m.status == UiMsgStatus.QUEUED) {
                // 将紧凑序号叠在气泡内容的右下角，并为文字预留横向空间；
                // 不再另起一行，短消息不会被状态提示拉高。
                Box {
                    Text(
                        m.text,
                        // 预留给右下角序号（也覆盖两位数队列位置）。
                        modifier = Modifier.padding(end = 30.dp),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = TextMain,
                    )
                    QueuedMarker(
                        position = (queuedIndex ?: 0) + 1,
                        label = queuedLabel(queuedIndex),
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            } else {
                Text(
                    m.text,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = TextMain,
                )
            }
            if (isUser) {
                UserMessageStatus(
                    message = m,
                    onRetry = onRetry,
                )
            }
            if (m.note.isNotEmpty() && !(isUser && m.status == UiMsgStatus.FAILED)) {
                Spacer(Modifier.height(6.dp))
                Text(m.note, fontSize = 11.sp, lineHeight = 15.sp, color = Faint)
            }
        }
    }
}

@Composable
private fun UserMessageStatus(
    message: UiMsg,
    onRetry: (Long) -> Unit,
) {
    when (message.status) {
        // 排队标记已叠加在消息正文上，完整说明由状态轨/队列面板提供。
        UiMsgStatus.QUEUED -> Unit

        UiMsgStatus.PROCESSING -> Unit

        UiMsgStatus.FAILED -> {
            if (message.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    message.note,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Bad,
                )
            }
            TextButton(
                onClick = { onRetry(message.id) },
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Bad),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${stringResource(R.string.queue_failed)} · ${stringResource(R.string.queue_retry)}",
                    fontSize = 12.sp,
                )
            }
        }

        UiMsgStatus.COMPLETE -> Unit
    }
}

@Composable
private fun queuedLabel(queuedIndex: Int?): String {
    return if ((queuedIndex ?: 0) == 0) {
        stringResource(R.string.queue_waiting)
    } else {
        stringResource(R.string.queue_waiting_ahead, queuedIndex ?: 0)
    }
}

@Composable
private fun QueuedMarker(
    position: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "#$position",
        modifier = Modifier
            .then(modifier)
            .semantics(mergeDescendants = true) {
                // 视觉上只保留紧凑序号；无障碍用户仍能得到完整队列信息。
                contentDescription = label
            }
            .padding(start = 3.dp, bottom = 1.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = Muted,
    )
}

@Composable
private fun QueueRail(
    busy: Boolean,
    queuedCount: Int,
    processingManual: Boolean,
    onClick: () -> Unit,
) {
    val hasQueue = queuedCount > 0
    val label = when {
        busy && hasQueue && processingManual ->
            stringResource(R.string.queue_replying_pending, queuedCount)
        hasQueue -> stringResource(R.string.queue_manual_pending, queuedCount)
        else -> stringResource(R.string.queue_replying)
    }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(Ink2.copy(alpha = 0.72f))
            .border(1.dp, Line, shape)
            .clickable(enabled = hasQueue, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                if (hasQueue) role = Role.Button
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = if (hasQueue) Gold else Muted,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = Gold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = if (hasQueue) Gold else Muted,
        )
        if (hasQueue) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Muted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queuedMessages: List<UiMsg>,
    onDismiss: () -> Unit,
    onCancel: (Long) -> Unit,
    onClear: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Ink2,
        contentColor = TextMain,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.queue_pending_title, queuedMessages.size),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = Muted,
                    )
                }
            }
            Text(
                stringResource(R.string.queue_pending_hint),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Muted,
            )
            Spacer(Modifier.height(10.dp))

            if (queuedMessages.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Muted,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.queue_pending_empty),
                        fontSize = 13.sp,
                        color = Muted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(queuedMessages, key = { it.id }) { message ->
                        PendingQueueRow(message = message, onCancel = onCancel)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onClear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Bad),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.queue_clear))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PendingQueueRow(
    message: UiMsg,
    onCancel: (Long) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(Ink3.copy(alpha = 0.62f))
            .border(1.dp, Line, shape)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message.text,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextMain,
        )
        TextButton(
            onClick = { onCancel(message.id) },
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Muted),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.queue_cancel), fontSize = 12.sp)
        }
    }
}
