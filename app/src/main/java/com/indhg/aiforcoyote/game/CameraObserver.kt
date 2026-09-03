package com.indhg.aiforcoyote.game

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 摄像头观察状态（UI 展示用）。 */
data class CameraState(
    val enabled: Boolean = false,
    val hasFrame: Boolean = false,
    val meanBrightness: Float = 0f,
    val dark: Boolean = false,
    val error: String = "",
)

/**
 * 摄像头观察器：CameraX 定时截帧，复刻桌面版 camera.py。
 * - 每 1.5s 一帧，JPEG 质量 80
 * - 平均亮度（Y 平面均值）< 20 判「画面黑暗」（桌面 dark_threshold=20）
 * - 权限缺失自动禁用不崩溃（可选依赖降级）
 * - 绑定 Activity 生命周期：退后台自动停、回前台续（CameraX 原生行为）
 */
class CameraObserver(private val context: Context) {

    companion object {
        private const val TAG = "CameraObs"
        private const val INTERVAL_MS = 1500L
        private const val DARK_THRESHOLD = 20f
        private const val JPEG_QUALITY = 80
    }

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var latestJpeg: ByteArray? = null

    @Volatile
    private var lastTs = 0L

    fun start(owner: LifecycleOwner) {
        if (_state.value.enabled) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _state.value = CameraState(error = context.getString(com.indhg.aiforcoyote.R.string.err_cam_perm))
            return
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy -> analyze(proxy) }
                p.unbindAll()
                // 前置摄像头：与屏幕同面，观察面对屏幕的玩家
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
                _state.value = _state.value.copy(enabled = true, error = "")
                Log.i(TAG, "摄像头已启动")
            } catch (e: Exception) {
                Log.w(TAG, "摄像头启动失败: ${e.message}")
                _state.value = CameraState(error = context.getString(com.indhg.aiforcoyote.R.string.err_cam_start, e.message ?: ""))
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        val p = provider
        provider = null
        latestJpeg = null
        lastTs = 0
        if (p != null) {
            try {
                ContextCompat.getMainExecutor(context).execute { p.unbindAll() }
            } catch (_: Exception) {
            }
        }
        _state.value = _state.value.copy(enabled = false, hasFrame = false, dark = false, error = "")
    }

    /** 最新帧 base64（供 vision 模型注入），无帧返回 null。 */
    fun base64(): String? {
        val jpeg = latestJpeg ?: return null
        return Base64.encodeToString(jpeg, Base64.NO_WRAP)
    }

    private fun analyze(proxy: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastTs < INTERVAL_MS) return // 限流：1.5s 一帧
            lastTs = now
            val mean = meanBrightness(proxy)
            val jpeg = proxyToJpeg(proxy)
            if (jpeg != null) latestJpeg = jpeg
            _state.value = CameraState(
                enabled = true,
                hasFrame = latestJpeg != null,
                meanBrightness = mean,
                dark = mean < DARK_THRESHOLD,
            )
        } catch (e: Exception) {
            Log.w(TAG, "截帧失败: ${e.message}")
        } finally {
            proxy.close()
        }
    }

    /** 平均亮度：YUV 的 Y 平面就是亮度，隔行隔列采样省 CPU。 */
    private fun meanBrightness(proxy: ImageProxy): Float {
        val yPlane = proxy.planes[0]
        val yBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val width = proxy.width
        val height = proxy.height
        var sum = 0L
        var count = 0
        var row = 0
        while (row < height) {
            val base = row * rowStride
            var col = 0
            while (col < width) {
                sum += (yBuffer.get(base + col * pixelStride).toInt() and 0xFF)
                count++
                col += 4
            }
            row += 4
        }
        return if (count > 0) sum.toFloat() / count else 0f
    }

    /** YUV_420_888 → NV21 → JPEG（按 rowStride/pixelStride 逐像素处理，适配各机型）。 */
    private fun proxyToJpeg(proxy: ImageProxy): ByteArray? {
        return try {
            val width = proxy.width
            val height = proxy.height
            val yPlane = proxy.planes[0]
            val uPlane = proxy.planes[1]
            val vPlane = proxy.planes[2]
            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer
            val nv21 = ByteArray(width * height * 3 / 2)
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride
            for (row in 0 until height) {
                val src = row * yRowStride
                val dst = row * width
                for (col in 0 until width) {
                    nv21[dst + col] = yBuffer.get(src + col * yPixelStride)
                }
            }
            val uvRowStride = uPlane.rowStride
            val uvPixelStride = uPlane.pixelStride
            val uvWidth = width / 2
            val uvHeight = height / 2
            var dst = width * height
            for (row in 0 until uvHeight) {
                val src = row * uvRowStride
                for (col in 0 until uvWidth) {
                    nv21[dst++] = vBuffer.get(src + col * uvPixelStride)
                    nv21[dst++] = uBuffer.get(src + col * uvPixelStride)
                }
            }
            val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val bos = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, bos)
            bos.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "JPEG 转换失败: ${e.message}")
            null
        }
    }
}
