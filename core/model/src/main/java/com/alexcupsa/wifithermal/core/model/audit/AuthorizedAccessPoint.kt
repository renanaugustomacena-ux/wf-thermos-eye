package com.alexcupsa.wifithermal.core.model.audit

import com.alexcupsa.wifithermal.core.model.SecurityType
import kotlinx.serialization.Serializable

/**
 * An access point the operator has explicitly marked as authorized to be
 * present on the corporate wireless. Anything observed in scope that does
 * not match the whitelist is a candidate for a [RogueAlert].
 *
 * BSSID is the primary key — SSID alone is unsafe (anyone can name a hotspot
 * "CorporateOffice"). The whitelist is BSSID-anchored, with SSID stored as a
 * label that the [RogueAlert.EvilTwin] detector cross-references.
 */
@Serializable
data class AuthorizedAccessPoint(
    val bssid: String,
    val ssid: String,
    val location: String?,
    val owner: String?,
    val deviceType: DeviceType,
    val expectedSecurity: SecurityType,
    val notes: String?,
    val authorizedAt: Long,
)

@Serializable
enum class DeviceType {
    ENTERPRISE_AP,
    CONSUMER_AP,
    GUEST_NETWORK,
    IOT_GATEWAY,
    PRINTER,
    OTHER,
}
