package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.ChannelWidth
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.model.WifiBand
import com.alexcupsa.wifithermal.core.model.WifiStandard
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.AuthorizedAccessPoint
import com.alexcupsa.wifithermal.core.model.audit.DeviceType
import com.alexcupsa.wifithermal.core.model.audit.RogueAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RogueDetectorTest {

    @Test
    fun `whitelisted bssid produces no alert`() {
        val results = listOf(scan(bssid = "AA:BB:CC:00:00:01", ssid = "Corp"))
        val whitelist = listOf(authorized(bssid = "AA:BB:CC:00:00:01", ssid = "Corp"))

        val alerts = RogueDetector.analyze(results, whitelist)

        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `unknown bssid with strong rssi and weak crypto is critical`() {
        val results = listOf(
            scan(
                bssid = "DE:AD:BE:EF:00:01",
                ssid = "RandomHotspot",
                rssi = -50,
                security = SecurityType.OPEN,
            ),
        )

        val alerts = RogueDetector.analyze(results, whitelist = emptyList())

        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertTrue(alert is RogueAlert.UnknownAccessPoint)
        assertEquals(AlertSeverity.CRITICAL, alert.severity)
    }

    @Test
    fun `unknown bssid far away with strong crypto is medium`() {
        val results = listOf(
            scan(
                bssid = "DE:AD:BE:EF:00:02",
                ssid = "Other",
                rssi = -82,
                security = SecurityType.WPA3_SAE,
            ),
        )

        val alerts = RogueDetector.analyze(results, whitelist = emptyList())

        assertEquals(1, alerts.size)
        assertEquals(AlertSeverity.MEDIUM, alerts.first().severity)
    }

    @Test
    fun `evil twin overrides unknown classification`() {
        val results = listOf(
            scan(bssid = "AA:BB:CC:00:00:01", ssid = "Corp", rssi = -45),
            scan(bssid = "FF:FF:FF:11:22:33", ssid = "Corp", rssi = -55), // clone of Corp
        )
        val whitelist = listOf(authorized(bssid = "AA:BB:CC:00:00:01", ssid = "Corp"))

        val alerts = RogueDetector.analyze(results, whitelist)

        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertTrue("expected EvilTwin, got $alert", alert is RogueAlert.EvilTwin)
        val twin = alert as RogueAlert.EvilTwin
        assertEquals("FF:FF:FF:11:22:33", twin.bssid)
        assertEquals("Corp", twin.impersonatedSsid)
        assertEquals("AA:BB:CC:00:00:01", twin.authorizedBssid)
        assertEquals(AlertSeverity.CRITICAL, twin.severity)
    }

    @Test
    fun `case insensitive bssid matching prevents bypass via lowercase`() {
        val results = listOf(scan(bssid = "aa:bb:cc:00:00:01", ssid = "Corp"))
        val whitelist = listOf(authorized(bssid = "AA:BB:CC:00:00:01", ssid = "Corp"))

        val alerts = RogueDetector.analyze(results, whitelist)

        assertTrue("scan bssid was treated as new despite case-only difference", alerts.isEmpty())
    }

    @Test
    fun `empty ssid does not trigger evil twin against whitelist`() {
        val results = listOf(scan(bssid = "FF:FF:FF:00:00:01", ssid = "", rssi = -50))
        val whitelist = listOf(authorized(bssid = "AA:BB:CC:00:00:01", ssid = ""))

        val alerts = RogueDetector.analyze(results, whitelist)

        // Hidden / empty SSID must not collide on empty string
        assertEquals(1, alerts.size)
        assertTrue(alerts.first() is RogueAlert.UnknownAccessPoint)
    }

    @Test
    fun `multiple unknown aps each get their own alert`() {
        val results = listOf(
            scan(bssid = "11:22:33:44:55:01", ssid = "A"),
            scan(bssid = "11:22:33:44:55:02", ssid = "B"),
            scan(bssid = "11:22:33:44:55:03", ssid = "C"),
        )

        val alerts = RogueDetector.analyze(results, whitelist = emptyList())

        assertEquals(3, alerts.size)
    }

    @Test
    fun `firstSeen and lastSeen use provided clock`() {
        val results = listOf(scan(bssid = "AA:BB:CC:DD:EE:FF", ssid = "X"))
        val now = 1_700_000_000_000L

        val alerts = RogueDetector.analyze(results, whitelist = emptyList(), nowMs = now)

        assertNotNull(alerts.firstOrNull())
        assertEquals(now, alerts.first().firstSeen)
        assertEquals(now, alerts.first().lastSeen)
    }

    private fun scan(
        bssid: String,
        ssid: String,
        rssi: Int = -55,
        security: SecurityType = SecurityType.WPA2_PSK,
    ) = ProcessedScanResult(
        ssid = ssid,
        bssid = bssid,
        rssi = rssi,
        smoothedRssi = rssi.toDouble(),
        frequency = 2412,
        channel = 1,
        channelWidth = ChannelWidth.MHZ_20,
        band = WifiBand.BAND_2_4_GHZ,
        security = security,
        standard = WifiStandard.WIFI_5_AC,
        signalQuality = SignalQuality.fromRssi(rssi),
        estimatedDistance = 0.0,
        isConnected = false,
        vendor = null,
        ageMs = 0L,
    )

    private fun authorized(bssid: String, ssid: String) = AuthorizedAccessPoint(
        bssid = bssid,
        ssid = ssid,
        location = null,
        owner = null,
        deviceType = DeviceType.ENTERPRISE_AP,
        expectedSecurity = SecurityType.WPA2_PSK,
        notes = null,
        authorizedAt = 0L,
    )
}
