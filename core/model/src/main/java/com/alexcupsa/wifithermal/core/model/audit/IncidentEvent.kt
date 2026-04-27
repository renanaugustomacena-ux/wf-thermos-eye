package com.alexcupsa.wifithermal.core.model.audit

import kotlinx.serialization.Serializable

/**
 * Persisted log entry of an audit finding. Append-only timeline used for
 * forensic review by IT/HR and for time-series anomaly analysis.
 *
 * `evidenceJson` is a free-form JSON blob with whatever extra detail the
 * detector wanted to record (capabilities string, channel, signal strength
 * series, OUI lookup result). Never put PII or credentials in this field.
 */
@Serializable
data class IncidentEvent(
    val id: Long,
    val timestamp: Long,
    val kind: IncidentKind,
    val bssid: String,
    val ssid: String?,
    val severity: AlertSeverity,
    val summary: String,
    val evidenceJson: String,
)

@Serializable
enum class IncidentKind {
    UNKNOWN_AP_OBSERVED,
    EVIL_TWIN_DETECTED,
    AP_WHITELISTED,
    AP_REMOVED_FROM_WHITELIST,
    SCOPE_LOADED,
    SCOPE_EXPIRED,
}
