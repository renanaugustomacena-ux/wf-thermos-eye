package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.AnomalyFlag
import com.alexcupsa.wifithermal.core.model.audit.AnomalyKind
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Reads recent [IncidentEvent] history and produces [AnomalyFlag]s for
 * suspicious time-series patterns the live [RogueDetector] cannot see.
 *
 * Stateless — caller passes the input window. Time inputs are configurable
 * to keep tests deterministic; production callers default to wall-clock.
 */
object AnomalyDetector {

    /** Default thresholds chosen for office-hours environments. */
    data class Thresholds(
        val burstWindowMs: Long = 30 * 60_000L, // 30 minutes
        val burstThreshold: Int = 5,
        val offHoursStartHour: Int = 22,
        val offHoursEndHour: Int = 6,
        val reemergenceGapMs: Long = 24 * 3_600_000L, // 24 hours
    )

    fun analyze(
        incidents: List<IncidentEvent>,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        thresholds: Thresholds = Thresholds(),
    ): List<AnomalyFlag> {
        if (incidents.isEmpty()) return emptyList()

        val byBssid = incidents.groupBy { it.bssid }
        val flags = mutableListOf<AnomalyFlag>()

        for ((bssid, events) in byBssid) {
            val sorted = events.sortedBy { it.timestamp }

            // Rule 1: burst — many sightings in a small window.
            val recent = sorted.filter { nowMs - it.timestamp <= thresholds.burstWindowMs }
            if (recent.size >= thresholds.burstThreshold) {
                flags += AnomalyFlag(
                    bssid = bssid,
                    kind = AnomalyKind.BURST_INTERMITTENT,
                    severity = AlertSeverity.HIGH,
                    description = "${recent.size} sightings in last ${thresholds.burstWindowMs / 60_000} min",
                    detectedAt = nowMs,
                    sampleCount = recent.size,
                )
            }

            // Rule 2: off-hours appearance — newest event landed in the
            // configured night window.
            val newest = sorted.last()
            val hour = ZonedDateTime.ofInstant(Instant.ofEpochMilli(newest.timestamp), zoneId).hour
            val inOffHours = if (thresholds.offHoursStartHour > thresholds.offHoursEndHour) {
                hour >= thresholds.offHoursStartHour || hour < thresholds.offHoursEndHour
            } else {
                hour in thresholds.offHoursStartHour until thresholds.offHoursEndHour
            }
            if (inOffHours) {
                flags += AnomalyFlag(
                    bssid = bssid,
                    kind = AnomalyKind.OFF_HOURS_APPEARANCE,
                    severity = AlertSeverity.MEDIUM,
                    description = "Latest sighting at ${"%02d".format(hour)}:xx (off-hours)",
                    detectedAt = nowMs,
                    sampleCount = sorted.size,
                )
            }

            // Rule 3: re-emergence — newest event came after a >24h gap from
            // the previous one. Reveals "phantom" rogues that drop off then
            // reappear later (pattern of someone bringing hardware in/out).
            if (sorted.size >= 2) {
                val previous = sorted[sorted.size - 2]
                val gap = newest.timestamp - previous.timestamp
                if (gap >= thresholds.reemergenceGapMs) {
                    flags += AnomalyFlag(
                        bssid = bssid,
                        kind = AnomalyKind.REEMERGENCE_AFTER_LONG_ABSENCE,
                        severity = AlertSeverity.HIGH,
                        description = "Returned after ${gap / 3_600_000} h absence",
                        detectedAt = nowMs,
                        sampleCount = sorted.size,
                    )
                }
            }
        }

        return flags
    }
}
