package com.indhg.aiforcoyote.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.indhg.aiforcoyote.MainViewModel
import com.indhg.aiforcoyote.ui.theme.Faint
import com.indhg.aiforcoyote.ui.theme.Gold
import com.indhg.aiforcoyote.ui.theme.Ink
import com.indhg.aiforcoyote.ui.theme.Line
import com.indhg.aiforcoyote.ui.theme.Muted
import com.indhg.aiforcoyote.ui.theme.TextMain
import java.io.File

/** 内置文件管理器式 DLC 导入：浏览全盘、多选 .zip/.md、一键导入；系统选择器兜底。 */
@Composable
fun FilePickerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val toast by vm.toast.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var hasAccess by remember { mutableStateOf(hasStorageAccess(context)) }
    var currentDir by remember { mutableStateOf(defaultStartDir()) }
    var entries by remember(currentDir, hasAccess) { mutableStateOf(listDir(currentDir)) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    // 从系统授权页/系统选择器回来时刷新权限与目录内容
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = hasStorageAccess(context)
                entries = listDir(currentDir)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val legacyPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAccess = granted
    }
    val sysLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importDlcUris(uris)
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
                .navigationBarsPadding(),
        ) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("← 返回", color = Muted) }
                Spacer(Modifier.weight(1f))
                Text("导入 DLC", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Gold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    sysLauncher.launch(
                        arrayOf(
                            "application/zip", "application/x-zip-compressed", "application/octet-stream",
                            "text/markdown", "text/plain",
                        ),
                    )
                }) { Text("系统选择器", fontSize = 12.sp, color = Muted) }
            }

            if (!hasAccess) {
                // 无权限：说明 + 授权入口 + 系统选择器兜底
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("需要存储访问权限", fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextMain)
                    Text(
                        "内置文件管理器需要「所有文件访问」权限（安卓 11+）才能浏览文件夹，"
                            + "授权后可像文件管理器一样从下载目录一路浏览全盘。",
                        fontSize = 12.sp, color = Muted,
                    )
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 30) {
                                context.startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            } else {
                                legacyPermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                    ) { Text("去授权", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                    Text(
                        "不想授权也可以用右上角「系统选择器」直接导入。",
                        fontSize = 11.sp, color = Faint,
                    )
                }
            } else {
                // 当前路径 + 上级
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        enabled = canGoUp(currentDir),
                        onClick = { currentDir.parentFile?.let { currentDir = it; selected = emptySet() } },
                    ) { Text("↑ 上级", color = if (canGoUp(currentDir)) Gold else Faint) }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        currentDir.path,
                        fontSize = 12.sp,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                // 文件列表
                LazyColumn(Modifier.weight(1f)) {
                    if (entries.isEmpty()) {
                        item {
                            Text("空文件夹", modifier = Modifier.padding(16.dp), fontSize = 12.sp, color = Faint)
                        }
                    }
                    items(entries) { f ->
                        val selectable = isDlcFile(f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (f.isDirectory) {
                                        currentDir = f
                                        selected = emptySet()
                                    } else if (selectable) {
                                        selected = if (f.absolutePath in selected) selected - f.absolutePath else selected + f.absolutePath
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (f.isDirectory) {
                                Text("📁", fontSize = 16.sp)
                            } else {
                                Checkbox(
                                    checked = f.absolutePath in selected,
                                    onCheckedChange = null,
                                    enabled = selectable,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Gold,
                                        uncheckedColor = Line,
                                        disabledCheckedColor = Faint,
                                        disabledUncheckedColor = Line,
                                    ),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                f.name,
                                fontSize = 13.sp,
                                color = when {
                                    f.isDirectory -> TextMain
                                    selectable -> Gold
                                    else -> Faint
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // 底部导入按钮
                Button(
                    onClick = {
                        vm.importDlcUris(selected.map { Uri.fromFile(File(it)) })
                        selected = emptySet()
                    },
                    enabled = selected.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Ink,
                        disabledContainerColor = Color(0x33F7D97A),
                        disabledContentColor = Faint,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(1.dp, Line, RoundedCornerShape(10.dp)),
                ) { Text("导入所选（${selected.size}）", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }
}

/** 是否已有存储浏览权限（11+ 看所有文件访问，10- 看读存储运行时权限）。 */
private fun hasStorageAccess(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

/** 默认起点：下载目录（不存在则退到外部存储根）。 */
private fun defaultStartDir(): File {
    val down = File(Environment.getExternalStorageDirectory(), "Download")
    return if (down.isDirectory) down else Environment.getExternalStorageDirectory()
}

/** 列出目录内容：文件夹在前、按名排序、隐藏文件过滤。 */
private fun listDir(dir: File): List<File> =
    dir.listFiles { f -> !f.name.startsWith(".") }
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        ?: emptyList()

/** 可导入文件：.zip 或 .md。 */
private fun isDlcFile(f: File): Boolean =
    f.isFile && (f.name.endsWith(".zip", ignoreCase = true) || f.name.endsWith(".md", ignoreCase = true))

/** 是否还能继续往上层走（封顶 /storage，避免无权限的根目录）。 */
private fun canGoUp(dir: File): Boolean {
    if (dir.parentFile == null) return false
    val path = dir.path.trimEnd('/')
    return path != "/storage" && path != "/"
}
