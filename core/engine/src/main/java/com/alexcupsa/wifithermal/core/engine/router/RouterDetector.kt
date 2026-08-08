package com.alexcupsa.wifithermal.core.engine.router

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

data class RouterInfo(
    val gatewayIp: String,
    val bssid: String?,
    val manufacturer: String?,
    val httpPort: Int?,
    val httpsPort: Int?,
    val sshPort: Int?,
    val telnetPort: Int?,
)

object RouterDetector {

    private val COMMON_GATEWAYS = listOf(
        "192.168.1.1",
        "192.168.0.1",
        "192.168.1.254",
        "192.168.0.254",
        "192.168.2.1",
        "10.0.0.1",
        "10.0.0.138",
        "10.1.1.1",
        "172.16.0.1",
    )

    private val ROUTER_PORTS = mapOf(
        "http" to listOf(80, 8080, 8000, 8888),
        "https" to listOf(443, 8443),
        "ssh" to listOf(22),
        "telnet" to listOf(23),
    )

    suspend fun detectGateway(): String? = withContext(Dispatchers.IO) {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return@withContext null

        for (iface in interfaces) {
            if (!iface.isUp || iface.isLoopback) continue
            if (iface.name.startsWith("wlan") || iface.name.startsWith("wifi")) {
                val addrs = iface.interfaceAddresses
                for (addr in addrs) {
                    val ip = addr.address
                    if (ip is java.net.Inet4Address && !ip.isLoopbackAddress) {
                        val gateway = deriveGatewayFromIp(ip.hostAddress)
                        if (gateway != null && isReachable(gateway)) {
                            return@withContext gateway
                        }
                    }
                }
            }
        }

        for (gateway in COMMON_GATEWAYS) {
            if (isReachable(gateway)) {
                return@withContext gateway
            }
        }

        null
    }

    private fun deriveGatewayFromIp(clientIp: String): String? {
        val parts = clientIp.split(".")
        if (parts.size != 4) return null
        return "${parts[0]}.${parts[1]}.${parts[2]}.1"
    }

    private suspend fun isReachable(ip: String, timeoutMs: Int = 1000): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                InetAddress.getByName(ip).isReachable(timeoutMs)
            }.getOrDefault(false)
        }

    suspend fun scanRouterPorts(gatewayIp: String): RouterInfo = withContext(Dispatchers.IO) {
        var httpPort: Int? = null
        var httpsPort: Int? = null
        var sshPort: Int? = null
        var telnetPort: Int? = null

        for (port in ROUTER_PORTS["http"].orEmpty()) {
            if (isPortOpen(gatewayIp, port)) {
                httpPort = port
                break
            }
        }

        for (port in ROUTER_PORTS["https"].orEmpty()) {
            if (isPortOpen(gatewayIp, port)) {
                httpsPort = port
                break
            }
        }

        for (port in ROUTER_PORTS["ssh"].orEmpty()) {
            if (isPortOpen(gatewayIp, port)) {
                sshPort = port
                break
            }
        }

        for (port in ROUTER_PORTS["telnet"].orEmpty()) {
            if (isPortOpen(gatewayIp, port)) {
                telnetPort = port
                break
            }
        }

        RouterInfo(
            gatewayIp = gatewayIp,
            bssid = null,
            manufacturer = null,
            httpPort = httpPort,
            httpsPort = httpsPort,
            sshPort = sshPort,
            telnetPort = telnetPort,
        )
    }

    private suspend fun isPortOpen(host: String, port: Int, timeoutMs: Int = 1000): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
                    true
                }
            }.getOrDefault(false)
        }

    fun extractGatewayFromBssid(bssid: String): String? {
        val cleanBssid = bssid.uppercase().replace(":", "").replace("-", "")
        if (cleanBssid.length != 12) return null

        val lastByte = cleanBssid.takeLast(2).toIntOrNull(16) ?: return null

        return when {
            lastByte <= 10 -> "192.168.1.1"
            lastByte <= 50 -> "192.168.0.1"
            else -> "192.168.1.254"
        }
    }
}
