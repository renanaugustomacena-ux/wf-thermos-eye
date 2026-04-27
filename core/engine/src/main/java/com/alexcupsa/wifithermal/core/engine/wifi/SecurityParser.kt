package com.alexcupsa.wifithermal.core.engine.wifi

import com.alexcupsa.wifithermal.core.model.SecurityType

/**
 * Parses the Android `ScanResult.capabilities` string into a [SecurityType].
 *
 * The capabilities string typically looks like `[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]`
 * or `[WPA2-EAP+FT/EAP-CCMP][RSN-EAP+FT/EAP-CCMP][ESS]`.
 *
 * Parsing order matters: we check from strongest to weakest so that a
 * capability string containing both WPA2 and WPA tags resolves to WPA2.
 */
object SecurityParser {

    /**
     * Parse a capabilities string into a [SecurityType].
     *
     * @param capabilities Raw capabilities string from a WiFi scan result.
     * @return The highest security type detected, or [SecurityType.UNKNOWN]
     *         if the string is empty or unrecognised.
     */
    fun parse(capabilities: String): SecurityType {
        val caps = capabilities.uppercase()
        return when {
            "WPA3" in caps || "SAE" in caps -> SecurityType.WPA3_SAE
            "WPA2-EAP" in caps || "RSN-EAP" in caps -> SecurityType.WPA2_EAP
            "WPA2" in caps || "RSN" in caps -> SecurityType.WPA2_PSK
            "WPA-EAP" in caps -> SecurityType.WPA_EAP
            "WPA" in caps -> SecurityType.WPA_PSK
            "WEP" in caps -> SecurityType.WEP
            "ESS" in caps && !caps.contains("WPA") && !caps.contains("WEP") &&
                !caps.contains("RSN") -> SecurityType.OPEN
            else -> SecurityType.UNKNOWN
        }
    }
}
