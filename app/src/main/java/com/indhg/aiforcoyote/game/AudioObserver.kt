package com.indhg.aiforcoyote.game

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 麦克风观察状态（UI 展示用）。 */
data class AudioState(
    val enabled: Boolean = false,
    val running: Boolean = false,
    val level: Double = 0.0,
    val silent: Boolean = true,
    val error: String = "",
)

/**
 * 麦克风观察器：AudioRecord 音量分级，复刻桌面版 audio.py 的呻吟分级（不做 whisper）。
 * - 16k 单声道，每 4s 一个窗口算 RMS（桌面 interval_s=4）
 * - 电平 ≥ 0.02 记声音；呻吟分级 high=阈值×8（惨叫收敛）/ low（普通呻吟加码），同档冷却 5s
 * - >90s 无声音记「持续无声」（呆滞判定用）
 * - 权限缺失自动禁用不崩溃
 */
class AudioObserver(
    private val context: Context,
    private val scope: CoroutineScope,
    val onMoan: (kind: String, level: Double) -> Unit = { _, _ -> },
) {

    companion object {
        private const val TAG = "AudioObs"
        private const val SAMPLE_RATE = 16000
        private const val WINDOW_S = 4
        private const val THRESHOLD = 0.02
        private const val MOAN_HIGH_MULTIPLE = 8.0
        private const val MOAN_COOLDOWN_S = 5.0
        private const val SILENCE_TIMEOUT_S = 90.0
    }

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var job: Job? = null
    private var lastSoundTs = 0.0
    private var lastMoanTs = 0.0
    private var lastMoanKind = ""

    fun start() {
        if (job?.isActive == true) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _state.value = AudioState(error = "麦克风权限未授予（仅聊天模式）")
            return
        }
        job = scope.launch(Dispatchers.IO) {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuf <= 0) {
                _state.value = AudioState(error = "麦克风不可用")
                return@launch
            }
            val rec = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2,
                )
            } catch (e: Exception) {
                _state.value = AudioState(error = "麦克风打开失败: ${e.message}")
                return@launch
            }
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = AudioState(error = "麦克风初始化失败")
                return@launch
            }
            try {
                rec.startRecording()
                _state.value = AudioState(enabled = true, running = true)
                Log.i(TAG, "麦克风监听已启动")
                val buf = ShortArray(minBuf)
                val window = ShortArray(SAMPLE_RATE * WINDOW_S)
                var filled = 0
                while (isActive) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    val copyLen = min(n, window.size - filled)
                    System.arraycopy(buf, 0, window, filled, copyLen)
                    filled += copyLen
                    if (filled >= window.size) {
                        processWindow(window)
                        filled = 0
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "录音异常: ${e.message}")
                _state.value = _state.value.copy(error = "录音异常: ${e.message}")
            } finally {
                try {
                    rec.stop()
                } catch (_: Exception) {
                }
                try {
                    rec.release()
                } catch (_: Exception) {
                }
                _state.value = _state.value.copy(running = false, enabled = false)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun processWindow(window: ShortArray) {
        var sum = 0.0
        for (v in window) {
            val f = v / 32768.0
            sum += f * f
        }
        val rms = sqrt(sum / window.size)
        _state.value = _state.value.copy(level = rms)
        val now = System.currentTimeMillis() / 1000.0
        if (rms >= THRESHOLD) {
            lastSoundTs = now
            // 呻吟分级（只做音量分级，不用转写）：同档冷却 5s 防刷屏
            val kind = if (rms >= THRESHOLD * MOAN_HIGH_MULTIPLE) "high" else "low"
            if (now - lastMoanTs >= MOAN_COOLDOWN_S || kind != lastMoanKind) {
                lastMoanTs = now
                lastMoanKind = kind
                Log.i(TAG, "呻吟信号: $kind (电平 $rms)")
                onMoan(kind, rms)
            }
        }
        _state.value = _state.value.copy(silent = now - lastSoundTs > SILENCE_TIMEOUT_S)
    }
}
