package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.AuthorizedAccessPoint
import com.alexcupsa.wifithermal.core.model.audit.RogueAlert

/**
 * Stateless rule engine. Given (in-scope scan results, current whitelist),
 * produces a flat list of [RogueAlert]s for this scan window.
 *
 * Inputs are assumed already filtered through [ScopeGuard], so any
 * BSSID arriving here is something the operator has authority over.
 *
 * Severity mapping:
 * - EvilTwin → CRITICAL (active impersonation, MITM threat)
 * - Unknown AP with strong RSSI close-by → HIGH (someone planted hardware)
 * - Unknown AP at low RSSI → MEDIUM (maybe drift or guest device)
 * - Unknown open / WEP / WPA-PSK AP regardless of RSSI → HIGH minimum
 *   (weak crypto on corporate floor is itself a finding)
 */
object RogueDetector {

    private const val CLOSE_RSSI_THRESHOLD = -65 // dBm; tighter than typical room edge

    fun analyze(
        inScopeResults: List<ProcessedScanResult>,
        whitelist: List<AuthorizedAccessPoint>,
        nowMs: Long = System.currentTimeMillis(),
    ): List<RogueAlert> {
        val whitelistedBssids = whitelist.mapTo(mutableSetOf()) { it.bssid.uppercase() }
        val whitelistBySsid = whitelist.groupBy { it.ssid }

        val alerts = mutableListOf<RogueAlert>()

        for (result in inScopeResults) {
            val bssidUpper = result.bssid.uppercase()

            if (bssidUpper in whitelistedBssids) {
                continue // authorized — nothing to do
            }

            // Evil twin: SSID matches a whitelisted entry but BSSID does not.
            val authorizedSiblings = whitelistBySsid[result.ssid].orEmpty()
            if (authorizedSiblings.isNotEmpty() && result.ssid.isNotEmpty()) {
                alerts += RogueAlert.EvilTwin(
                    bssid = result.bssid,
                    impersonatedSsid = result.ssid,
                    authorizedBssid = authorizedSiblings.first().bssid,
                    rssi = result.rssi,
                    vendor = result.vendor,
                    firstSeen = nowMs,
                    lastSeen = nowMs,
                    severity = AlertSeverity.CRITICAL,
                )
                continue // a single AP cannot be both evil twin and unknown
            }

            // Unknown AP — severity depends on signal proximity and crypto posture.
            alerts += RogueAlert.UnknownAccessPoint(
                bssid = result.bssid,
                ssid = result.ssid,
                rssi = result.rssi,
                vendor = result.vendor,
                security = result.security,
                firstSeen = nowMs,
                lastSeen = nowMs,
                severity = severityForUnknown(result),
            )
        }

        return alerts
    }

    private fun severityForUnknown(result: ProcessedScanResult): AlertSeverity {
        val close = result.rssi >= CLOSE_RSSI_THRESHOLD
        val weakCrypto = result.security in WEAK_CRYPTO
        return when {
            close && weakCrypto -> AlertSeverity.CRITICAL
            weakCrypto -> AlertSeverity.HIGH
            close -> AlertSeverity.HIGH
            else -> AlertSeverity.MEDIUM
        }
    }

    private val WEAK_CRYPTO = setOf(
        com.alexcupsa.wifithermal.core.model.SecurityType.OPEN,
        com.alexcupsa.wifithermal.core.model.SecurityType.WEP,
        com.alexcupsa.wifithermal.core.model.SecurityType.WPA_PSK,
    )
}
