package com.indhg.aiforcoyote.util

import java.net.Inet4Address
import java.net.NetworkInterface

/** 取手机当前局域网 IPv4（无 Wi-Fi 时回退 127.0.0.1）。 */
object LanIp {
    fun get(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
                ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }
}
