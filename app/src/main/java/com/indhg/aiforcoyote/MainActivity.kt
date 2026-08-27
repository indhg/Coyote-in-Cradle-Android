package com.indhg.aiforcoyote

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.indhg.aiforcoyote.ui.ChatScreen
import com.indhg.aiforcoyote.ui.SettingsScreen
import com.indhg.aiforcoyote.ui.theme.CoyoteTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* 结果由观察器按权限自检降级 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 观察期间屏幕常亮（任务书要求：本 App 前台常驻）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 摄像头 + 麦克风权限（拒绝则对应观察自动禁用，仅聊天不崩溃）
        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        // 前台开观察、退后台暂停
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.startObservation(this)
                Lifecycle.Event.ON_STOP -> vm.stopObservation()
                else -> {}
            }
        })
        setContent {
            CoyoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSettings by remember { mutableStateOf(false) }
                    if (showSettings) {
                        SettingsScreen(vm = vm, onBack = { showSettings = false })
                    } else {
                        ChatScreen(vm = vm, onOpenSettings = { showSettings = true })
                    }
                }
            }
        }
    }
}
