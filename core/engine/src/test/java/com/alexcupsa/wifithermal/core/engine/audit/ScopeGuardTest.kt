package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.model.ChannelWidth
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.model.WifiBand
import com.alexcupsa.wifithermal.core.model.WifiStandard
import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopeGuardTest {

    @Test
    fun `empty scope rejects everything`() {
        val scope = scope(ssidPatterns = emptyList(), bssidPrefixes = emptyList())
        val result = scan(bssid = "AA:BB:CC:00:00:01", ssid = "Anything")

        assertFalse(ScopeGuard.inScope(result, scope))
    }

    @Test
    fun `exact ssid match passes scope`() {
        val scope = scope(ssidPatterns = listOf("CorpInternal"), bssidPrefixes = emptyList())
        val matching = scan(bssid = "AA:BB:CC:00:00:01", ssid = "CorpInternal")
        val nonMatching = scan(bssid = "AA:BB:CC:00:00:02", ssid = "Other")

        assertTrue(ScopeGuard.inScope(matching, scope))
        assertFalse(ScopeGuard.inScope(nonMatching, scope))
    }

    @Test
    fun `wildcard suffix ssid pattern matches prefix`() {
        val scope = scope(ssidPatterns = listOf("Corp*"), bssidPrefixes = emptyList())

        assertTrue(ScopeGuard.inScope(scan(bssid = "01:02:03:04:05:06", ssid = "CorpInternal"), scope))
        assertTrue(ScopeGuard.inScope(scan(bssid = "01:02:03:04:05:07", ssid = "CorpGuest"), scope))
        assertFalse(ScopeGuard.inScope(scan(bssid = "01:02:03:04:05:08", ssid = "Other"), scope))
    }

    @Test
    fun `bssid prefix match is case insensitive`() {
        val scope = scope(ssidPatterns = emptyList(), bssidPrefixes = listOf("aa:bb:cc"))

        assertTrue(ScopeGuard.inScope(scan(bssid = "AA:BB:CC:00:00:01", ssid = "x"), scope))
        assertTrue(ScopeGuard.inScope(scan(bssid = "aa:bb:cc:00:00:02", ssid = "x"), scope))
        assertFalse(ScopeGuard.inScope(scan(bssid = "DE:AD:BE:EF:00:00", ssid = "x"), scope))
    }

    @Test
    fun `ssid OR bssid match - either path qualifies`() {
        val scope = scope(
            ssidPatterns = listOf("CorpGuest"),
            bssidPrefixes = listOf("AA:BB:CC"),
        )

        // BSSID matches but SSID does not
        assertTrue(ScopeGuard.inScope(scan(bssid = "AA:BB:CC:00:00:01", ssid = "Random"), scope))
        // SSID matches but BSSID does not
        assertTrue(ScopeGuard.inScope(scan(bssid = "DE:AD:BE:EF:00:00", ssid = "CorpGuest"), scope))
        // Neither matches
        assertFalse(ScopeGuard.inScope(scan(bssid = "DE:AD:BE:EF:00:00", ssid = "Random"), scope))
    }

    @Test
    fun `filter drops out-of-scope entries`() {
        val scope = scope(ssidPatterns = listOf("Corp*"), bssidPrefixes = emptyList())
        val results = listOf(
            scan(bssid = "01:02:03:04:05:01", ssid = "CorpA"),
            scan(bssid = "01:02:03:04:05:02", ssid = "Other"),
            scan(bssid = "01:02:03:04:05:03", ssid = "CorpB"),
        )

        val filtered = ScopeGuard.filter(results, scope)

        assertEquals(2, filtered.size)
        assertEquals(setOf("CorpA", "CorpB"), filtered.map { it.ssid }.toSet())
    }

    private fun scope(ssidPatterns: List<String>, bssidPrefixes: List<String>) = AuthorizationScope(
        organizationName = "Test Co",
        authorizedBy = "Test Owner",
        authorizedAt = 0L,
        expiresAt = null,
        ssidPatterns = ssidPatterns,
        bssidPrefixes = bssidPrefixes,
        notes = "",
    )

    private fun scan(bssid: String, ssid: String) = ProcessedScanResult(
        ssid = ssid,
        bssid = bssid,
        rssi = -55,
        smoothedRssi = -55.0,
        frequency = 2412,
        channel = 1,
        channelWidth = ChannelWidth.MHZ_20,
        band = WifiBand.BAND_2_4_GHZ,
        security = SecurityType.WPA2_PSK,
        standard = WifiStandard.WIFI_5_AC,
        signalQuality = SignalQuality.fromRssi(-55),
        estimatedDistance = 0.0,
        isConnected = false,
        vendor = null,
        ageMs = 0L,
    )
}
