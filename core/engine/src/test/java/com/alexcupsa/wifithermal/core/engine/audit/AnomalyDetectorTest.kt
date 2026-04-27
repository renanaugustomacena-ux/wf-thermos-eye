package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.AnomalyKind
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import com.alexcupsa.wifithermal.core.model.audit.IncidentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AnomalyDetectorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `empty input produces no flags`() {
        val flags = AnomalyDetector.analyze(emptyList(), nowMs = 1_700_000_000_000L, zoneId = zone)
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `single sighting produces no flags`() {
        val now = ZonedDateTime.of(2026, 4, 27, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        val flags = AnomalyDetector.analyze(
            incidents = listOf(incident("AA:BB:CC:00:00:01", now)),
            nowMs = now,
            zoneId = zone,
        )
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `5 sightings within 30 minutes triggers BURST_INTERMITTENT`() {
        val now = ZonedDateTime.of(2026, 4, 27, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        val incidents = (0 until 5).map { i ->
            incident("AA:BB:CC:00:00:01", now - i * 60_000L)
        }
        val flags = AnomalyDetector.analyze(incidents, nowMs = now, zoneId = zone)
        val burst = flags.firstOrNull { it.kind == AnomalyKind.BURST_INTERMITTENT }
        assertNotNull(burst)
        assertEquals(AlertSeverity.HIGH, burst!!.severity)
        assertEquals(5, burst.sampleCount)
    }

    @Test
    fun `4 sightings within 30 minutes does not trigger burst`() {
        val now = ZonedDateTime.of(2026, 4, 27, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        val incidents = (0 until 4).map { i ->
            incident("AA:BB:CC:00:00:01", now - i * 60_000L)
        }
        val flags = AnomalyDetector.analyze(incidents, nowMs = now, zoneId = zone)
        assertTrue(flags.none { it.kind == AnomalyKind.BURST_INTERMITTENT })
    }

    @Test
    fun `sighting at 02 AM triggers OFF_HOURS_APPEARANCE`() {
        val night = ZonedDateTime.of(2026, 4, 27, 2, 30, 0, 0, zone).toInstant().toEpochMilli()
        val flags = AnomalyDetector.analyze(
            incidents = listOf(incident("AA:BB:CC:00:00:01", night)),
            nowMs = night,
            zoneId = zone,
        )
        assertNotNull(flags.firstOrNull { it.kind == AnomalyKind.OFF_HOURS_APPEARANCE })
    }

    @Test
    fun `sighting at 14 PM does not trigger off hours`() {
        val day = ZonedDateTime.of(2026, 4, 27, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        val flags = AnomalyDetector.analyze(
            incidents = listOf(incident("AA:BB:CC:00:00:01", day)),
            nowMs = day,
            zoneId = zone,
        )
        assertTrue(flags.none { it.kind == AnomalyKind.OFF_HOURS_APPEARANCE })
    }

    @Test
    fun `sighting at 22 30 triggers off hours when window crosses midnight`() {
        val late = ZonedDateTime.of(2026, 4, 27, 22, 30, 0, 0, zone).toInstant().toEpochMilli()
        val flags = AnomalyDetector.analyze(
            incidents = listOf(incident("AA:BB:CC:00:00:01", late)),
            nowMs = late,
            zoneId = zone,
        )
        assertNotNull(flags.firstOrNull { it.kind == AnomalyKind.OFF_HOURS_APPEARANCE })
    }

    @Test
    fun `gap of 25 hours triggers REEMERGENCE`() {
        val now = ZonedDateTime.of(2026, 4, 27, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        val incidents = listOf(
            incident("AA:BB:CC:00:00:01", now - 26 * 3_600_000L),
            incident("AA:BB:CC:00:00:01", now),
        )
        val flags = AnomalyDetector.analyze(incidents, nowMs = now, zoneId = zone)
        assertNotNull(flags.firstOrNull { it.kind == AnomalyKind.REEMERGENCE_AFTER_LONG_ABSENCE })
    }

    @Test
    fun `gap of 12 hours does not trigger reemergence`() {
        val now = ZonedDateTime.of(2026, 4, 27, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        val incidents = listOf(
            incident("AA:BB:CC:00:00:01", now - 12 * 3_600_000L),
            incident("AA:BB:CC:00:00:01", now),
        )
        val flags = AnomalyDetector.analyze(incidents, nowMs = now, zoneId = zone)
        assertTrue(flags.none { it.kind == AnomalyKind.REEMERGENCE_AFTER_LONG_ABSENCE })
    }

    @Test
    fun `each bssid evaluated independently`() {
        val now = ZonedDateTime.of(2026, 4, 27, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        val incidents = listOf(
            incident("AA:BB:CC:00:00:01", now - 1_000),
            incident("AA:BB:CC:00:00:01", now - 2_000),
            incident("AA:BB:CC:00:00:01", now - 3_000),
            incident("AA:BB:CC:00:00:01", now - 4_000),
            incident("AA:BB:CC:00:00:01", now - 5_000),
            incident("DD:EE:FF:00:00:02", now - 6_000),
        )
        val flags = AnomalyDetector.analyze(incidents, nowMs = now, zoneId = zone)
        val burstFlags = flags.filter { it.kind == AnomalyKind.BURST_INTERMITTENT }
        assertEquals(1, burstFlags.size)
        assertEquals("AA:BB:CC:00:00:01", burstFlags.first().bssid)
    }

    private fun incident(bssid: String, ts: Long) = IncidentEvent(
        id = 0,
        timestamp = ts,
        kind = IncidentKind.UNKNOWN_AP_OBSERVED,
        bssid = bssid,
        ssid = "Test",
        severity = AlertSeverity.HIGH,
        summary = "obs",
        evidenceJson = "{}",
    )
}
