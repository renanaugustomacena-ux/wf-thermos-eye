package com.alexcupsa.wifithermal.core.model.audit

import kotlinx.serialization.Serializable

/**
 * Time-series anomaly raised by [com.alexcupsa.wifithermal.core.engine.audit.AnomalyDetector]
 * over the persisted [IncidentEvent] history.
 *
 * Distinct from a live [RogueAlert]: anomalies are patterns observed across
 * multiple sightings, not the immediate presence of an in-scope rogue.
 */
@Serializable
data class AnomalyFlag(
    val bssid: String,
    val kind: AnomalyKind,
    val severity: AlertSeverity,
    val description: String,
    val detectedAt: Long,
    val sampleCount: Int,
)

@Serializable
enum class AnomalyKind {
    /** >N sightings of same BSSID in a short window = flapping AP, possibly evasive. */
    BURST_INTERMITTENT,

    /** AP appeared during off-hours (e.g. 22:00-06:00) = suspicious time of attachment. */
    OFF_HOURS_APPEARANCE,

    /** AP returned after >24h absence = "phantom" rogue. */
    REEMERGENCE_AFTER_LONG_ABSENCE,
}
