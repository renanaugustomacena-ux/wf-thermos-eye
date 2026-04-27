package com.alexcupsa.wifithermal.core.engine.security

import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SecurityAuditResult
import com.alexcupsa.wifithermal.core.model.SecurityFinding
import com.alexcupsa.wifithermal.core.model.SecurityScore
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.Severity

/**
 * Security auditor that analyses WiFi scan results for potential risks.
 *
 * Checks performed:
 * 1. **Open networks** (no encryption) -- Severity.CRITICAL
 * 2. **WEP encryption** (trivially breakable) -- Severity.CRITICAL
 * 3. **WPA with TKIP** (deprecated, vulnerable to MICHAEL attack) -- Severity.HIGH
 * 4. **Hidden SSIDs** (empty SSID, may indicate attempt to evade detection) -- Severity.MEDIUM
 * 5. **Default SSID names** (indicates factory-default configuration) -- Severity.MEDIUM
 * 6. **Potential rogue APs** (same SSID, different vendor) -- Severity.HIGH
 *
 * The overall security score is computed from the severity distribution of
 * findings and the ratio of secure (WPA2+) to insecure APs.
 */
object SecurityAuditor {

    /** Common default SSID patterns that indicate an unconfigured router. */
    private val DEFAULT_SSID_PATTERNS = listOf(
        Regex("^linksys$", RegexOption.IGNORE_CASE),
        Regex("^netgear$", RegexOption.IGNORE_CASE),
        Regex("^NETGEAR\\d+$"),
        Regex("^default$", RegexOption.IGNORE_CASE),
        Regex("^dlink$", RegexOption.IGNORE_CASE),
        Regex("^D-Link$"),
        Regex("^TP-LINK_[A-F0-9]+$"),
        Regex("^TP-Link_[A-F0-9]+$"),
        Regex("^ASUS$"),
        Regex("^ASUS_[A-F0-9]+$"),
        Regex("^HOME-[A-F0-9]+$"),
        Regex("^HUAWEI-[A-Za-z0-9]+$"),
        Regex("^FRITZ!Box \\d+$"),
        Regex("^SETUP$", RegexOption.IGNORE_CASE),
        Regex("^xfinitywifi$", RegexOption.IGNORE_CASE),
        Regex("^ATT[A-Za-z0-9]+$"),
        Regex("^DIRECT-[A-Za-z0-9]+$"),
        Regex("^AndroidAP$"),
    )

    /**
     * Perform a security audit of the given scan results.
     *
     * @param scanResults List of processed scan results from the latest scan.
     * @return [SecurityAuditResult] containing findings sorted by severity
     *         (critical first), counts per security type, and an overall score.
     */
    fun audit(scanResults: List<ProcessedScanResult>): SecurityAuditResult {
        val findings = mutableListOf<SecurityFinding>()

        findings.addAll(checkOpenNetworks(scanResults))
        findings.addAll(checkWepNetworks(scanResults))
        findings.addAll(checkWpaPsk(scanResults))
        findings.addAll(checkHiddenSsids(scanResults))
        findings.addAll(checkDefaultSsids(scanResults))
        findings.addAll(checkRogueAps(scanResults))

        // Sort findings: CRITICAL > HIGH > MEDIUM > LOW > INFO
        val sortedFindings = findings.sortedBy { it.severity.ordinal }

        val openCount = scanResults.count { it.security == SecurityType.OPEN }
        val wepCount = scanResults.count { it.security == SecurityType.WEP }
        val wpaCount = scanResults.count {
            it.security == SecurityType.WPA_PSK || it.security == SecurityType.WPA_EAP
        }
        val wpa2Count = scanResults.count {
            it.security == SecurityType.WPA2_PSK || it.security == SecurityType.WPA2_EAP
        }
        val wpa3Count = scanResults.count { it.security == SecurityType.WPA3_SAE }

        val overallScore = computeOverallScore(sortedFindings, scanResults.size)

        return SecurityAuditResult(
            findings = sortedFindings,
            totalAps = scanResults.size,
            openCount = openCount,
            wepCount = wepCount,
            wpaCount = wpaCount,
            wpa2Count = wpa2Count,
            wpa3Count = wpa3Count,
            overallScore = overallScore,
        )
    }

    // --- Individual checks ---------------------------------------------------

    private fun checkOpenNetworks(
        results: List<ProcessedScanResult>,
    ): List<SecurityFinding> = results
        .filter { it.security == SecurityType.OPEN }
        .map { ap ->
            SecurityFinding(
                severity = Severity.CRITICAL,
                bssid = ap.bssid,
                ssid = ap.ssid,
                title = "Open network (no encryption)",
                description = "Network '${ap.ssid.ifEmpty { "<hidden>" }}' (${ap.bssid}) " +
                    "has no encryption. All traffic is transmitted in cleartext " +
                    "and can be captured by any device within range.",
                recommendation = "Enable WPA3-SAE or WPA2-PSK with a strong passphrase. " +
                    "If this is a guest network, use WPA2-PSK with client isolation.",
            )
        }

    private fun checkWepNetworks(
        results: List<ProcessedScanResult>,
    ): List<SecurityFinding> = results
        .filter { it.security == SecurityType.WEP }
        .map { ap ->
            SecurityFinding(
                severity = Severity.CRITICAL,
                bssid = ap.bssid,
                ssid = ap.ssid,
                title = "WEP encryption (broken)",
                description = "Network '${ap.ssid}' (${ap.bssid}) uses WEP encryption, " +
                    "which can be cracked in under 5 minutes with freely available " +
                    "tools (aircrack-ng). WEP provides no meaningful security.",
                recommendation = "Upgrade immediately to WPA3-SAE or at minimum WPA2-PSK " +
                    "with AES-CCMP. Replace the access point if it does not support WPA2.",
            )
        }

    private fun checkWpaPsk(
        results: List<ProcessedScanResult>,
    ): List<SecurityFinding> = results
        .filter { it.security == SecurityType.WPA_PSK || it.security == SecurityType.WPA_EAP }
        .map { ap ->
            SecurityFinding(
                severity = Severity.HIGH,
                bssid = ap.bssid,
                ssid = ap.ssid,
                title = "WPA with TKIP (deprecated)",
                description = "Network '${ap.ssid}' (${ap.bssid}) uses WPA with TKIP, " +
                    "which is deprecated and vulnerable to the Beck-Tews and " +
                    "Ohigashi-Morii attacks. Modern clients may refuse to connect.",
                recommendation = "Upgrade to WPA2-PSK with AES-CCMP or WPA3-SAE.",
            )
        }

    private fun checkHiddenSsids(
        results: List<ProcessedScanResult>,
    ): List<SecurityFinding> = results
        .filter { it.ssid.isBlank() }
        .map { ap ->
            SecurityFinding(
                severity = Severity.MEDIUM,
                bssid = ap.bssid,
                ssid = "",
                title = "Hidden SSID",
                description = "AP ${ap.bssid} broadcasts a hidden (empty) SSID. " +
                    "This provides no real security benefit because the SSID is " +
                    "revealed in probe responses and association frames. " +
                    "Hidden SSIDs can also indicate an attempt to avoid casual detection.",
                recommendation = "Hidden SSIDs are generally discouraged. " +
                    "If the network is legitimate, consider making the SSID visible " +
                    "and relying on strong encryption instead.",
            )
        }

    private fun checkDefaultSsids(
        results: List<ProcessedScanResult>,
    ): List<SecurityFinding> = results
        .filter { ap -> DEFAULT_SSID_PATTERNS.any { it.matches(ap.ssid) } }
        .map { ap ->
            SecurityFinding(
                severity = Severity.MEDIUM,
                bssid = ap.bssid,
                ssid = ap.ssid,
                title = "Default SSID name",
                description = "Network '${ap.ssid}' (${ap.bssid}) appears to use a " +
                    "factory-default SSID, suggesting the device may still have " +
                    "default credentials, unpatched firmware, or a misconfigured " +
                    "admin interface.",
                recommendation = "Change the SSID to a unique name. Verify that the " +
                    "admin password has been changed from the factory default and " +
                    "that the firmware is up to date.",
            )
        }

    /**
     * Detect potential rogue APs: multiple APs advertising the same SSID but
     * from different vendors (by OUI). This is a common evil-twin indicator.
     *
     * We only flag SSIDs where there are at least 2 distinct vendors, and we
     * do not flag empty/hidden SSIDs.
     */
    private fun checkRogueAps(
        results: List<ProcessedScanResult>,
    ): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        val bySsid = results
            .filter { it.ssid.isNotBlank() }
            .groupBy { it.ssid }

        for ((ssid, aps) in bySsid) {
            val vendors = aps.mapNotNull { it.vendor }.toSet()
            if (vendors.size >= 2) {
                // Flag each AP from the minority vendor(s) as a potential rogue
                val vendorCounts = aps.groupBy { it.vendor ?: "Unknown" }
                val majorityVendor = vendorCounts.maxByOrNull { it.value.size }?.key

                for (ap in aps) {
                    val apVendor = ap.vendor ?: "Unknown"
                    if (apVendor != majorityVendor) {
                        findings.add(
                            SecurityFinding(
                                severity = Severity.HIGH,
                                bssid = ap.bssid,
                                ssid = ssid,
                                title = "Potential rogue AP (evil twin)",
                                description = "AP ${ap.bssid} (vendor: $apVendor) advertises " +
                                    "SSID '$ssid' but has a different vendor than the " +
                                    "majority of APs with the same SSID (majority vendor: " +
                                    "$majorityVendor). This could be an evil twin attack.",
                                recommendation = "Verify this AP is authorised. If not, " +
                                    "remove it from the network and investigate. Consider " +
                                    "enabling 802.11w (Protected Management Frames) and " +
                                    "deploying a WIDS/WIPS solution.",
                            )
                        )
                    }
                }
            }
        }

        return findings
    }

    // --- Overall score -------------------------------------------------------

    private fun computeOverallScore(
        findings: List<SecurityFinding>,
        totalAps: Int,
    ): SecurityScore {
        if (totalAps == 0) return SecurityScore.EXCELLENT

        val criticalCount = findings.count { it.severity == Severity.CRITICAL }
        val highCount = findings.count { it.severity == Severity.HIGH }
        val mediumCount = findings.count { it.severity == Severity.MEDIUM }

        // Weighted penalty score: critical=10, high=5, medium=2, low=1
        val penalty = criticalCount * 10 + highCount * 5 + mediumCount * 2

        // Normalise against total APs so that a single open AP in 100 APs
        // does not tank the score as much as 1 open AP in 2 APs.
        val normalisedPenalty = penalty.toDouble() / totalAps

        return when {
            normalisedPenalty >= 8.0 -> SecurityScore.CRITICAL
            normalisedPenalty >= 4.0 -> SecurityScore.POOR
            normalisedPenalty >= 2.0 -> SecurityScore.FAIR
            normalisedPenalty >= 0.5 -> SecurityScore.GOOD
            else -> SecurityScore.EXCELLENT
        }
    }
}
