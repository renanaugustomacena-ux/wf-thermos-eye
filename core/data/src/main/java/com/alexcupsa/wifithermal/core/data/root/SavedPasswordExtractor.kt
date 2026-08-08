package com.alexcupsa.wifithermal.core.data.root

import com.alexcupsa.wifithermal.core.common.root.RootResult
import com.alexcupsa.wifithermal.core.common.root.RootShell
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

data class SavedWifiCredential(
    val ssid: String,
    val bssid: String?,
    val password: String?,
    val keyMgmt: String,
    val source: String,
)

@Singleton
class SavedPasswordExtractor @Inject constructor(
    private val rootShell: RootShell,
) {
    companion object {
        private const val WPA_SUPPLICANT_CONF = "/data/misc/wifi/wpa_supplicant.conf"
        private const val WIFI_CONFIG_STORE = "/data/misc/wifi/WifiConfigStore.xml"
        private const val SOFTAP_CONFIG = "/data/misc/wifi/softap.conf"
    }

    suspend fun extract(): RootResult<List<SavedWifiCredential>> {
        if (!rootShell.isRooted()) {
            return RootResult.NoRoot
        }

        val results = mutableListOf<SavedWifiCredential>()

        val configStore = extractFromConfigStore()
        if (configStore is RootResult.Success) {
            results.addAll(configStore.data)
        }

        val wpaSupplicant = extractFromWpaSupplicant()
        if (wpaSupplicant is RootResult.Success) {
            results.addAll(wpaSupplicant.data)
        }

        val softAp = extractSoftApPassword()
        if (softAp is RootResult.Success) {
            results.add(softAp.data)
        }

        return RootResult.Success(results.distinctBy { it.ssid to it.password })
    }

    private suspend fun extractFromConfigStore(): RootResult<List<SavedWifiCredential>> {
        val content = rootShell.readFile(WIFI_CONFIG_STORE)
        if (content !is RootResult.Success) {
            return when (content) {
                is RootResult.Error -> RootResult.Error(content.code, content.message)
                RootResult.NoRoot -> RootResult.NoRoot
                RootResult.Timeout -> RootResult.Timeout
                else -> RootResult.Error(-1, "Unknown error")
            }
        }

        return runCatching {
            val credentials = mutableListOf<SavedWifiCredential>()
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(InputSource(StringReader(content.data)))
            doc.documentElement.normalize()

            val networkList = doc.getElementsByTagName("Network")
            for (i in 0 until networkList.length) {
                val network = networkList.item(i) as? Element ?: continue
                val wifiConfig = findChildElement(network, "WifiConfiguration") ?: continue

                var ssid: String? = null
                var bssid: String? = null
                var preSharedKey: String? = null
                var keyMgmt: String = "UNKNOWN"

                val strings = wifiConfig.getElementsByTagName("string")
                for (j in 0 until strings.length) {
                    val str = strings.item(j) as? Element ?: continue
                    when (str.getAttribute("name")) {
                        "SSID" -> ssid = str.textContent?.removeSurrounding("\"")
                        "BSSID" -> bssid = str.textContent?.takeIf { it != "any" }
                        "PreSharedKey" -> preSharedKey = str.textContent?.removeSurrounding("\"")
                        "ConfigKey" -> {
                            val configKey = str.textContent.orEmpty()
                            keyMgmt = when {
                                "WPA_PSK" in configKey -> "WPA-PSK"
                                "WPA3_SAE" in configKey -> "WPA3-SAE"
                                "WPA_EAP" in configKey -> "WPA-EAP"
                                "WEP" in configKey -> "WEP"
                                "NONE" in configKey -> "OPEN"
                                else -> configKey.substringAfterLast("\"").trim()
                            }
                        }
                    }
                }

                if (ssid != null && preSharedKey != null) {
                    credentials.add(
                        SavedWifiCredential(
                            ssid = ssid,
                            bssid = bssid,
                            password = preSharedKey,
                            keyMgmt = keyMgmt,
                            source = "WifiConfigStore.xml",
                        )
                    )
                }
            }
            RootResult.Success(credentials)
        }.getOrElse { e ->
            RootResult.Error(-1, "XML parse error: ${e.message}")
        }
    }

    private fun findChildElement(parent: Element, tagName: String): Element? {
        val children = parent.getElementsByTagName(tagName)
        return if (children.length > 0) children.item(0) as? Element else null
    }

    private suspend fun extractFromWpaSupplicant(): RootResult<List<SavedWifiCredential>> {
        if (!rootShell.fileExists(WPA_SUPPLICANT_CONF)) {
            return RootResult.Success(emptyList())
        }

        val content = rootShell.readFile(WPA_SUPPLICANT_CONF)
        if (content !is RootResult.Success) {
            return when (content) {
                is RootResult.Error -> RootResult.Error(content.code, content.message)
                RootResult.NoRoot -> RootResult.NoRoot
                RootResult.Timeout -> RootResult.Timeout
                else -> RootResult.Error(-1, "Unknown error")
            }
        }

        return runCatching {
            val credentials = mutableListOf<SavedWifiCredential>()
            val networkBlocks = extractNetworkBlocks(content.data)

            for (block in networkBlocks) {
                val ssid = extractValue(block, "ssid")?.removeSurrounding("\"")
                val psk = extractValue(block, "psk")?.removeSurrounding("\"")
                val bssid = extractValue(block, "bssid")?.takeIf { it != "any" }
                val keyMgmtRaw = extractValue(block, "key_mgmt") ?: "WPA-PSK"

                if (ssid != null && psk != null) {
                    credentials.add(
                        SavedWifiCredential(
                            ssid = ssid,
                            bssid = bssid,
                            password = psk,
                            keyMgmt = keyMgmtRaw,
                            source = "wpa_supplicant.conf",
                        )
                    )
                }
            }
            RootResult.Success(credentials)
        }.getOrElse { e ->
            RootResult.Error(-1, "Parse error: ${e.message}")
        }
    }

    private fun extractNetworkBlocks(content: String): List<String> {
        val blocks = mutableListOf<String>()
        var depth = 0
        val current = StringBuilder()
        var inNetwork = false

        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("network={") || trimmed == "network={") {
                inNetwork = true
                depth = 1
                current.clear()
                current.appendLine(trimmed)
            } else if (inNetwork) {
                current.appendLine(trimmed)
                depth += trimmed.count { it == '{' }
                depth -= trimmed.count { it == '}' }
                if (depth <= 0) {
                    blocks.add(current.toString())
                    inNetwork = false
                }
            }
        }
        return blocks
    }

    private fun extractValue(block: String, key: String): String? {
        val pattern = Regex("""^\s*$key\s*=\s*(.+)$""", RegexOption.MULTILINE)
        return pattern.find(block)?.groupValues?.get(1)?.trim()
    }

    private suspend fun extractSoftApPassword(): RootResult<SavedWifiCredential> {
        if (!rootShell.fileExists(SOFTAP_CONFIG)) {
            return RootResult.Error(-1, "SoftAP config not found")
        }

        val content = rootShell.readFile(SOFTAP_CONFIG)
        if (content !is RootResult.Success) {
            return when (content) {
                is RootResult.Error -> RootResult.Error(content.code, content.message)
                RootResult.NoRoot -> RootResult.NoRoot
                RootResult.Timeout -> RootResult.Timeout
                else -> RootResult.Error(-1, "Unknown error")
            }
        }

        return runCatching {
            val lines = content.data.lines()
            val ssid = lines.getOrNull(0)?.trim() ?: "Unknown"
            val password = lines.getOrNull(2)?.trim()

            if (password.isNullOrBlank()) {
                RootResult.Error(-1, "No SoftAP password found")
            } else {
                RootResult.Success(
                    SavedWifiCredential(
                        ssid = ssid,
                        bssid = null,
                        password = password,
                        keyMgmt = "HOTSPOT",
                        source = "softap.conf",
                    )
                )
            }
        }.getOrElse { e ->
            RootResult.Error(-1, "SoftAP parse error: ${e.message}")
        }
    }
}
