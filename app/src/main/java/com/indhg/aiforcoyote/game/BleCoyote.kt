package com.indhg.aiforcoyote.game

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 设备连接状态（UI 展示用）。 */
data class DeviceState(
    val status: String = "disconnected", // disconnected / scanning / connecting / connected
    val battery: Int? = null,
    val error: String = "",
)

/**
 * 郊狼 3.0 BLE 直连驱动（官方蓝牙协议 coyote/v3，无需郊狼 App / 中继）。
 * - 服务 0x180C / 写 0x150A / 通知 0x150B；电量 0x180A / 0x1500
 * - B0 指令：每 100ms 一条 20 字节帧（两通道强度 + 各 4 组频率/强度波形数据）
 * - BF 指令：软上限 + 平衡参数，重连后必须重写（官方警告，断电保存）
 * - 强度：UI 0-100 → 设备 0-200（×2，对齐官方 App 手感），绝对设置方式 0b11
 */
@SuppressLint("MissingPermission") // 运行时权限由 UI 层保证
class BleCoyote(
    context: Context,
    private val scope: CoroutineScope,
    val onDisconnect: () -> Unit = {},
) : DeviceOps {

    companion object {
        private const val TAG = "BleCoyote"
        private const val DEVICE_NAME = "47L121000"
        private const val SCAN_TIMEOUT_MS = 15_000L
        private const val STREAM_INTERVAL_MS = 100L
        private const val RECONNECT_DELAY_MS = 3_000L
        private val SERVICE = UUID.fromString("0000180C-0000-1000-8000-00805f9b34fb")
        private val WRITE_CHAR = UUID.fromString("0000150A-0000-1000-8000-00805f9b34fb")
        private val NOTIFY_CHAR = UUID.fromString("0000150B-0000-1000-8000-00805f9b34fb")
        private val BATTERY_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
        private val BATTERY_CHAR = UUID.fromString("00001500-0000-1000-8000-00805f9b34fb")
        private val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    // ---------- 每通道运行时状态 ----------
    private class ChState {
        var strength = 0                 // UI 0-100
        var dirty = true                 // 强度待下发
        var frames: List<String> = emptyList()  // 官方波形帧（8 hex/帧）
        var idx = 0                      // 流式游标（每 B0 前进 2 帧）
        var untilMs = 0L                 // pulse 结束时间（0 = pulse_hold 无限）
    }

    private val channels = ConcurrentHashMap<String, ChState>().apply {
        put("A", ChState())
        put("B", ChState())
    }
    private val tempJobs = ConcurrentHashMap<String, Job>()

    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var streamJob: Job? = null
    private var streaming = false
    private var scanCb: ScanCallback? = null

    /** 用户/程序想保持连接（用户手动断开时置 false，停止自动重连）。 */
    private var wantConnection = false

    /** 波形帧数据（assets/waveforms.json，与 RelayDevice 同源）。 */
    private val waveByName: Map<String, List<String>> by lazy {
        try {
            val text = context.assets.open("waveforms.json").bufferedReader().use { it.readText() }
            val json = kotlinx.serialization.json.Json.parseToJsonElement(text).let {
                (it as kotlinx.serialization.json.JsonObject)
            }
            json.entries.mapNotNull { (name, v) ->
                val obj = v as kotlinx.serialization.json.JsonObject
                val frames = obj["frames"]?.let { f ->
                    (f as kotlinx.serialization.json.JsonArray).map { el ->
                        (el as kotlinx.serialization.json.JsonPrimitive).content
                    }
                } ?: return@mapNotNull null
                name to frames
            }.toMap()
        } catch (e: Exception) {
            Log.w(TAG, "波形数据加载失败: ${e.message}")
            emptyMap()
        }
    }

    // ---------- 连接 ----------
    fun connect() {
        wantConnection = true
        if (_state.value.status == "scanning" || _state.value.status == "connecting") return
        val a = adapter ?: run {
            _state.value = _state.value.copy(error = "本机无蓝牙")
            return
        }
        _state.value = DeviceState("scanning")
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanCb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // 不用名字过滤器（广播包常缺名字，过滤器会漏掉设备），扫到后手动比对
                val advertisedName = result.scanRecord?.deviceName ?: result.device.name
                Log.i(TAG, "扫描到 BLE 设备 ${result.device.address} ($advertisedName)")
                if (advertisedName == DEVICE_NAME) {
                    stopScan()
                    _state.value = DeviceState("connecting")
                    connectGatt(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _state.value = DeviceState("disconnected", error = "扫描失败 code=$errorCode")
            }
        }
        a.bluetoothLeScanner?.startScan(null, settings, scanCb)
        scope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_state.value.status == "scanning") {
                stopScan()
                _state.value = DeviceState("disconnected", error = "未找到郊狼（BLE 名 $DEVICE_NAME）")
            }
        }
    }

    fun disconnect() {
        wantConnection = false
        stopScan()
        try {
            gatt?.disconnect()
        } catch (_: Exception) {
        }
        closeGatt()
    }

    private fun stopScan() {
        scanCb?.let { cb ->
            try {
                adapter?.bluetoothLeScanner?.stopScan(cb)
            } catch (_: Exception) {
            }
            scanCb = null
        }
    }

    private fun connectGatt(device: BluetoothDevice) {
        closeGatt()
        gatt = device.connectGatt(appContext, false, gattCb, BluetoothDevice.TRANSPORT_LE)
    }

    private fun closeGatt() {
        try {
            gatt?.close()
        } catch (_: Exception) {
        }
        gatt = null
        writeChar = null
        streamJob?.cancel()
        streamJob = null
        streaming = false
    }

    private val gattCb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "已连接，发现服务…")
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "连接断开 newState=$newState status=$status")
                onBleLost()
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "服务发现失败 status=$status")
                return
            }
            val service = g.getService(SERVICE)
            writeChar = service?.getCharacteristic(WRITE_CHAR)
            val notify = service?.getCharacteristic(NOTIFY_CHAR)
            if (writeChar == null || notify == null) {
                _state.value = DeviceState("disconnected", error = "特征值缺失，可能不是郊狼 3.0")
                disconnect()
                return
            }
            if (!enableNotify(g, notify)) {
                _state.value = DeviceState("disconnected", error = "通知订阅失败")
                disconnect()
                return
            }
            val battery = g.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_CHAR)
            if (battery != null) enableNotify(g, battery)
            writeBf() // 重连必写软上限（官方警告）
            // 复位强度为 0（断开时本地位已清零，dirty 已置位）
            _state.value = DeviceState("connected")
            ensureStreaming()
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid) {
                NOTIFY_CHAR -> handleNotify(characteristic.value)
                BATTERY_CHAR -> {
                    val v = characteristic.value
                    if (v != null && v.isNotEmpty()) {
                        _state.value = _state.value.copy(battery = v[0].toInt() and 0xFF)
                    }
                }
            }
        }
    }

    private fun enableNotify(g: BluetoothGatt, ch: BluetoothGattCharacteristic): Boolean {
        if (!g.setCharacteristicNotification(ch, true)) return false
        val descriptor = ch.getDescriptor(CCCD) ?: return false
        return if (Build.VERSION.SDK_INT >= 33) {
            g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            g.writeDescriptor(descriptor)
        }
    }

    /** B1 回应：序列号 + 两通道当前设备强度（0-200）。 */
    private fun handleNotify(data: ByteArray?) {
        if (data == null || data.size < 4 || (data[0].toInt() and 0xFF) != 0xB1) return
        val seq = data[1].toInt() and 0xFF
        val sa = data[2].toInt() and 0xFF
        val sb = data[3].toInt() and 0xFF
        Log.i(TAG, "B1 强度回传 seq=$seq A=$sa B=$sb")
    }

    private fun onBleLost() {
        stopScan()
        closeGatt()
        _state.value = DeviceState("disconnected", battery = _state.value.battery)
        onDisconnect()
        // 断线自动重连（用户主动断开时不重连）
        if (wantConnection) {
            scope.launch {
                delay(RECONNECT_DELAY_MS)
                if (wantConnection && _state.value.status == "disconnected") connect()
            }
        }
    }

    /** BF：软上限 200/200 + 频率平衡 200 + 强度平衡 200（对齐 Howl 默认）。 */
    private fun writeBf() {
        write(
            byteArrayOf(
                0xBF.toByte(), 200.toByte(), 200.toByte(), 200.toByte(),
                200.toByte(), 200.toByte(), 200.toByte(),
            ),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        )
    }

    private fun write(payload: ByteArray, writeType: Int): Boolean {
        val g = gatt ?: return false
        val c = writeChar ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                g.writeCharacteristic(c, payload, writeType) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                c.writeType = writeType
                @Suppress("DEPRECATION")
                c.value = payload
                @Suppress("DEPRECATION")
                g.writeCharacteristic(c)
            }
        } catch (e: Exception) {
            Log.w(TAG, "写入失败: ${e.message}")
            false
        }
    }

    // ---------- B0 流式循环 ----------
    private fun ensureStreaming() {
        if (streaming || _state.value.status != "connected") return
        streaming = true
        streamJob = scope.launch {
            try {
                while (isActive && streaming) {
                    if (hasWork()) {
                        writeB0() // 失败时 dirty 保持，下一帧重试
                    } else {
                        break
                    }
                    delay(STREAM_INTERVAL_MS)
                }
            } finally {
                streaming = false
            }
        }
    }

    private fun hasWork(): Boolean {
        val now = System.currentTimeMillis()
        return channels.values.any { it.dirty || (it.frames.isNotEmpty() && (it.untilMs == 0L || now < it.untilMs)) }
    }

    /** 组一帧 B0（20 字节）：强度变化用绝对设置 0b11 + 序列号 1，波形 4 组/通道。 */
    private fun writeB0(): Boolean {
        val a = channels["A"] ?: return false
        val b = channels["B"] ?: return false
        val now = System.currentTimeMillis()
        val out = ByteArray(20)
        out[0] = 0xB0.toByte()
        var mode = 0
        var seq = 0
        if (a.dirty) {
            mode = mode or (0b11 shl 2) // A 高两位 = 绝对
            out[2] = (a.strength * 2).coerceIn(0, 200).toByte()
            seq = 1
        }
        if (b.dirty) {
            mode = mode or 0b11 // B 低两位 = 绝对
            out[3] = (b.strength * 2).coerceIn(0, 200).toByte()
            seq = 1
        }
        out[1] = ((seq shl 4) or mode).toByte()
        fillChannel(out, 4, 8, isB = false, c = a, now = now)
        fillChannel(out, 12, 16, isB = true, c = b, now = now)
        val ok = write(out, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        if (ok) {
            a.dirty = false
            b.dirty = false
        }
        return ok
    }

    /** 每通道 4 组 (频率,强度) = 2 个波形帧（每帧 2 组）；无效数据（强度 101）→ 设备丢弃该通道。 */
    private fun fillChannel(out: ByteArray, freqOff: Int, intOff: Int, isB: Boolean, c: ChState, now: Long) {
        val active = c.frames.isNotEmpty() && (c.untilMs == 0L || now < c.untilMs)
        if (!active) {
            for (i in 0 until 4) {
                out[freqOff + i] = 0
                out[intOff + i] = 101
            }
            return
        }
        val base = if (isB) 8 else 0
        for (j in 0 until 4) {
            val frame = c.frames[(c.idx + j / 2) % c.frames.size]
            val cs = base + (j % 2) * 4
            out[freqOff + j] = frame.substring(cs, cs + 2).toInt(16).toByte()
            out[intOff + j] = frame.substring(cs + 2, cs + 4).toInt(16).toByte()
        }
        c.idx = (c.idx + 2) % c.frames.size
    }

    // ---------- DeviceOps ----------
    override suspend fun send(op: String, channel: String?, value: Int?, pattern: String?, durationS: Int?): Boolean {
        val ch = channel ?: return false
        val c = channels[ch] ?: return false
        when (op) {
            "hold_strength", "temp_strength" -> {
                c.strength = (value ?: 0).coerceIn(0, 100)
                c.dirty = true
                if (op == "temp_strength") scheduleTempRevert(ch, (durationS ?: 3).coerceIn(1, 30))
            }
            "add_strength" -> {
                val delta = value ?: return false
                c.strength = (c.strength + delta).coerceIn(0, 100)
                c.dirty = true
            }
            "pulse_hold", "pulse" -> {
                val frames = waveByName[pattern ?: ""] ?: run {
                    Log.w(TAG, "未知波形: $pattern")
                    return false
                }
                c.frames = frames
                c.idx = 0
                c.untilMs = if (op == "pulse") {
                    System.currentTimeMillis() + (durationS ?: 6).coerceIn(3, 10) * 1000L
                } else 0L
            }
            "clear" -> {
                c.frames = emptyList()
                c.strength = 0
                c.dirty = true
            }
            else -> return false
        }
        ensureStreaming()
        return true
    }

    override suspend fun clearAll() {
        channels.values.forEach {
            it.frames = emptyList()
            it.strength = 0
            it.dirty = true
        }
        tempJobs.values.forEach { it.cancel() }
        tempJobs.clear()
        ensureStreaming()
    }

    override fun needsLoopResend(): Boolean = false // 流式循环天然维持，无需 30s 重发

    /** 通道是否有活跃波形（双通道保底用）。 */
    fun waveActive(ch: String): Boolean {
        val c = channels[ch] ?: return false
        if (c.frames.isEmpty()) return false
        val now = System.currentTimeMillis()
        return c.untilMs == 0L || now < c.untilMs
    }

    /** 临时强度：时长到后归零（桌面语义）。 */
    private fun scheduleTempRevert(ch: String, durationS: Int) {
        tempJobs.remove(ch)?.cancel()
        tempJobs[ch] = scope.launch {
            delay(durationS * 1000L)
            val c = channels[ch] ?: return@launch
            c.strength = 0
            c.dirty = true
            ensureStreaming()
        }
    }
}
