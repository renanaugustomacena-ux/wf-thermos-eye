package com.alexcupsa.wifithermal.core.model.audit

import com.alexcupsa.wifithermal.core.model.SecurityType
import kotlinx.serialization.Serializable

/**
 * A finding produced by the audit pipeline. Each variant represents a
 * distinct policy violation with its own evidence shape.
 *
 * All variants are produced from passive observation of public 802.11
 * beacons / probe responses. No active probing, no client deauth, no
 * frame injection.
 */
@Serializable
sealed interface RogueAlert {
    val bssid: String
    val firstSeen: Long
    val lastSeen: Long
    val severity: AlertSeverity

    /** BSSID observed in scope but not in the operator-managed whitelist. */
    @Serializable
    data class UnknownAccessPoint(
        override val bssid: String,
        val ssid: String,
        val rssi: Int,
        val vendor: String?,
        val security: SecurityType,
        override val firstSeen: Long,
        override val lastSeen: Long,
        override val severity: AlertSeverity,
    ) : RogueAlert

    /**
     * SSID matches an authorized AP but BSSID does not — someone is
     * advertising a clone of the corporate network, almost certainly to
     * man-in-the-middle clients.
     */
    @Serializable
    data class EvilTwin(
        override val bssid: String,
        val impersonatedSsid: String,
        val authorizedBssid: String,
        val rssi: Int,
        val vendor: String?,
        override val firstSeen: Long,
        override val lastSeen: Long,
        override val severity: AlertSeverity,
    ) : RogueAlert
}

@Serializable
enum class AlertSeverity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
