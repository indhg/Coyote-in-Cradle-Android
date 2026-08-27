package com.indhg.aiforcoyote.relay

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.net.URLEncoder

/** 中继/配对状态。 */
data class RelayState(
    val status: String = "disconnected", // disconnected / connecting / waiting / paired
    val controllerId: String? = null,
    val clientId: String? = null,        // 被控方（郊狼 App）ID
    val slotId: String? = null,          // 设备 slotId（从 devices.snapshot 取）
    val pairUrl: String = "",
)

/**
 * 控制方客户端：连接本机内嵌中继，复刻桌面版 relay_client.py。
 * 自动重连；断线后由上层清零设备。
 */
class CoyoteController(
    private val url: String = "ws://127.0.0.1:9998",
    private val reconnectDelayMs: Long = 3_000,
    val onDisconnect: () -> Unit = {},
) : WebSocketClient(URI(url)) {

    private val json = Json { ignoreUnknownKeys = true }

    private val relayPort: Int = URI(url).port.let { if (it > 0) it else 9998 }

    private val _state = MutableStateFlow(RelayState())
    val state: StateFlow<RelayState> = _state.asStateFlow()

    private val slotByClient = mutableMapOf<String, String>()

    var running = false
        private set

    /** 常驻连接 + 自动重连（在协程里循环调用）。 */
    fun connectLoop() {
        if (running) return
        running = true
        Thread {
            while (running) {
                try {
                    setState(_state.value.copy(status = "connecting"))
                    connectBlocking()
                    break // 连接成功，之后由 onClose 回调决定重连
                } catch (e: Exception) {
                    Log.w(TAG, "中继连接失败: ${e.message}，${reconnectDelayMs}ms 后重试")
                    try {
                        Thread.sleep(reconnectDelayMs)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }.start()
    }

    fun stop() {
        running = false
        try {
            closeBlocking()
        } catch (_: Exception) {
        }
    }

    /** 下发一帧控制方消息：{"type":"message","clientId":被控方,"data":...}。 */
    fun sendData(data: JsonObject): Boolean {
        if (!isOpen) return false
        val cid = _state.value.clientId ?: return false
        val frame = buildJsonObject {
            put("type", JsonPrimitive("message"))
            put("clientId", JsonPrimitive(cid))
            put("data", data)
        }
        try {
            send(frame.toString())
            return true
        } catch (e: Exception) {
            Log.w(TAG, "发送失败: ${e.message}")
            return false
        }
    }

    private fun setState(next: RelayState) {
        _state.value = next
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        Log.i(TAG, "已连接中继 $url")
    }

    override fun onMessage(message: String) {
        val frame = try {
            json.parseToJsonElement(message).jsonObject
        } catch (_: Exception) {
            return
        }
        when (frame["type"]?.jsonPrimitive?.contentOrNull) {
            "hello" -> {
                val cid = frame["clientId"]?.jsonPrimitive?.contentOrNull ?: return
                Log.i(TAG, "拿到控制方 ID: $cid")
                // 用手机局域网 IP 生成配对地址（郊狼 App 可能拒绝 127.0.0.1）；
                // ws 链接整体 URL 编码，与桌面版 build_pair_url 完全一致
                val host = com.indhg.aiforcoyote.util.LanIp.get()
                val ws = "ws://$host:$relayPort?tid=$cid"
                setState(
                    _state.value.copy(
                        status = "waiting",
                        controllerId = cid,
                        pairUrl = "https://dungeon-lab.cn/s/?v=1&action=socket&url=" + URLEncoder.encode(ws, "UTF-8"),
                    )
                )
            }
            "client_attached" -> {
                val cid = frame["clientId"]?.jsonPrimitive?.contentOrNull ?: return
                Log.i(TAG, "郊狼 App 接入: $cid")
                setState(
                    _state.value.copy(
                        status = "paired",
                        clientId = cid,
                        slotId = slotByClient[cid],
                    )
                )
            }
            "client_disconnected" -> {
                val cid = frame["clientId"]?.jsonPrimitive?.contentOrNull
                Log.i(TAG, "郊狼 App 断开: $cid")
                slotByClient.remove(cid)
                setState(_state.value.copy(status = "waiting", clientId = null, slotId = null))
                onDisconnect()
            }
            "controller_disconnected" -> {
                Log.i(TAG, "控制方被中继断开")
                setState(_state.value.copy(status = "disconnected", clientId = null, slotId = null))
                onDisconnect()
            }
            "error" -> {
                Log.w(TAG, "中继错误帧: ${frame["code"]?.jsonPrimitive?.contentOrNull} ${frame["message"]?.jsonPrimitive?.contentOrNull}")
            }
            "message" -> handleMessage(frame)
        }
    }

    /** 上行消息：设备快照取 slotId。 */
    private fun handleMessage(frame: JsonObject) {
        val cid = frame["clientId"]?.jsonPrimitive?.contentOrNull ?: _state.value.clientId ?: return
        val data = frame["data"]?.jsonObject ?: return
        if (data["t"]?.jsonPrimitive?.contentOrNull != "ev") return
        when (data["ev"]?.jsonPrimitive?.contentOrNull) {
            "devices.snapshot" -> {
                val devices = data["devices"]?.jsonArray ?: return
                val slot = devices.firstOrNull()?.jsonObject?.get("slotId")?.jsonPrimitive?.contentOrNull
                Log.i(TAG, "设备快照: slotId=$slot")
                if (slot != null) {
                    slotByClient[cid] = slot
                    setState(_state.value.copy(slotId = slot))
                }
            }
            "slots.patch" -> Log.i(TAG, "槽位状态更新")
            "custom.action" -> Log.i(TAG, "郊狼 App 反馈按钮: ${data["action"]?.jsonPrimitive?.contentOrNull}")
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        Log.w(TAG, "中继断开 码=$code 原因=$reason，${reconnectDelayMs}ms 后重连")
        slotByClient.clear()
        setState(_state.value.copy(status = "disconnected", clientId = null, slotId = null))
        onDisconnect()
        if (running) {
            Thread {
                Thread.sleep(reconnectDelayMs)
                if (running) connectLoop()
            }.start()
        }
    }

    override fun onError(ex: Exception) {
        Log.w(TAG, "WS 错误: ${ex.message}")
    }

    companion object {
        private const val TAG = "CoyoteCtl"
    }
}
