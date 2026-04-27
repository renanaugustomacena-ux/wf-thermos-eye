package com.alexcupsa.wifithermal.core.engine.wifi

import com.alexcupsa.wifithermal.core.model.ChannelWidth
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.model.WifiBand
import com.alexcupsa.wifithermal.core.model.WifiStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for WiFi analysis engine objects: [ChannelMapper], [SecurityParser],
 * and [ChannelAnalyzer].
 *
 * All tests use JUnit 4 and are fully self-contained with no shared mutable state.
 */
class WifiAnalysisTest {

    // =========================================================================
    // Helper
    // =========================================================================

    private fun buildAp(
        ssid: String = "Test",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        rssi: Int = -50,
        frequency: Int = 2412,
        channel: Int = 1,
        band: WifiBand = WifiBand.BAND_2_4_GHZ,
        security: SecurityType = SecurityType.WPA2_PSK,
    ): ProcessedScanResult = ProcessedScanResult(
        ssid = ssid,
        bssid = bssid,
        rssi = rssi,
        smoothedRssi = rssi.toDouble(),
        frequency = frequency,
        channel = channel,
        channelWidth = ChannelWidth.MHZ_20,
        band = band,
        security = security,
        standard = WifiStandard.WIFI_5_AC,
        signalQuality = SignalQuality.EXCELLENT,
        estimatedDistance = 3.0,
    )

    // =========================================================================
    // ChannelMapper — frequencyToChannel
    // =========================================================================

    @Test
    fun frequencyToChannel_2412_returns_channel_1() {
        assertEquals(1, ChannelMapper.frequencyToChannel(2412))
    }

    @Test
    fun frequencyToChannel_2437_returns_channel_6() {
        assertEquals(6, ChannelMapper.frequencyToChannel(2437))
    }

    @Test
    fun frequencyToChannel_2462_returns_channel_11() {
        assertEquals(11, ChannelMapper.frequencyToChannel(2462))
    }

    @Test
    fun frequencyToChannel_2484_returns_channel_14() {
        assertEquals(14, ChannelMapper.frequencyToChannel(2484))
    }

    @Test
    fun frequencyToChannel_5180_returns_channel_36() {
        assertEquals(36, ChannelMapper.frequencyToChannel(5180))
    }

    @Test
    fun frequencyToChannel_5240_returns_channel_48() {
        assertEquals(48, ChannelMapper.frequencyToChannel(5240))
    }

    @Test
    fun frequencyToChannel_5745_returns_channel_149() {
        assertEquals(149, ChannelMapper.frequencyToChannel(5745))
    }

    @Test
    fun frequencyToChannel_5825_returns_channel_165() {
        assertEquals(165, ChannelMapper.frequencyToChannel(5825))
    }

    @Test
    fun frequencyToChannel_5955_6ghz_returns_channel_1() {
        // 6 GHz band: (5955 - 5950) / 5 = 1
        assertEquals(1, ChannelMapper.frequencyToChannel(5955))
    }

    @Test
    fun frequencyToChannel_unrecognised_returns_negative_1() {
        assertEquals(-1, ChannelMapper.frequencyToChannel(9999))
    }

    // =========================================================================
    // ChannelMapper — channelToFrequency
    // =========================================================================

    @Test
    fun channelToFrequency_ch1_2g_returns_2412() {
        assertEquals(2412, ChannelMapper.channelToFrequency(1, WifiBand.BAND_2_4_GHZ))
    }

    @Test
    fun channelToFrequency_ch14_2g_returns_2484() {
        assertEquals(2484, ChannelMapper.channelToFrequency(14, WifiBand.BAND_2_4_GHZ))
    }

    @Test
    fun channelToFrequency_ch36_5g_returns_5180() {
        assertEquals(5180, ChannelMapper.channelToFrequency(36, WifiBand.BAND_5_GHZ))
    }

    // =========================================================================
    // ChannelMapper — bandFromFrequency
    // =========================================================================

    @Test
    fun bandFromFrequency_2412_returns_2_4ghz() {
        assertEquals(WifiBand.BAND_2_4_GHZ, ChannelMapper.bandFromFrequency(2412))
    }

    @Test
    fun bandFromFrequency_5180_returns_5ghz() {
        assertEquals(WifiBand.BAND_5_GHZ, ChannelMapper.bandFromFrequency(5180))
    }

    // =========================================================================
    // ChannelMapper — NON_OVERLAPPING_2_4
    // =========================================================================

    @Test
    fun nonOverlapping24_contains_1_6_11() {
        assertEquals(listOf(1, 6, 11), ChannelMapper.NON_OVERLAPPING_2_4)
    }

    // =========================================================================
    // SecurityParser
    // =========================================================================

    @Test
    fun parse_wpa3_sae() {
        assertEquals(SecurityType.WPA3_SAE, SecurityParser.parse("[WPA3-SAE][ESS]"))
    }

    @Test
    fun parse_wpa2_psk() {
        assertEquals(
            SecurityType.WPA2_PSK,
            SecurityParser.parse("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"),
        )
    }

    @Test
    fun parse_wpa2_eap() {
        assertEquals(
            SecurityType.WPA2_EAP,
            SecurityParser.parse("[WPA2-EAP+FT/EAP-CCMP][ESS]"),
        )
    }

    @Test
    fun parse_wpa_psk() {
        assertEquals(SecurityType.WPA_PSK, SecurityParser.parse("[WPA-PSK-CCMP][ESS]"))
    }

    @Test
    fun parse_wpa_eap() {
        assertEquals(SecurityType.WPA_EAP, SecurityParser.parse("[WPA-EAP][ESS]"))
    }

    @Test
    fun parse_wep() {
        assertEquals(SecurityType.WEP, SecurityParser.parse("[WEP][ESS]"))
    }

    @Test
    fun parse_open() {
        assertEquals(SecurityType.OPEN, SecurityParser.parse("[ESS]"))
    }

    @Test
    fun parse_empty_returns_unknown() {
        assertEquals(SecurityType.UNKNOWN, SecurityParser.parse(""))
    }

    @Test
    fun parse_mixed_wpa2_and_wpa_returns_wpa2_strongest_wins() {
        // WPA2 is checked before WPA, so a capability string with both resolves to WPA2
        assertEquals(
            SecurityType.WPA2_PSK,
            SecurityParser.parse("[WPA2-PSK-CCMP][WPA-PSK-TKIP][ESS]"),
        )
    }

    @Test
    fun parse_sae_keyword_returns_wpa3() {
        // The parser checks for "SAE" keyword regardless of the WPA3 prefix
        assertEquals(SecurityType.WPA3_SAE, SecurityParser.parse("[RSN-SAE-CCMP][ESS]"))
    }

    // =========================================================================
    // ChannelAnalyzer
    // =========================================================================

    @Test
    fun analyze_emptyList_returns_all_channels_with_zero_aps() {
        val result = ChannelAnalyzer.analyze(emptyList())

        // 2.4 GHz: channels 1-13
        assertEquals(13, result.channels2g.size)
        for (ch in result.channels2g) {
            assertEquals(0, ch.apCount)
        }

        // 5 GHz: 25 standard channels
        assertEquals(25, result.channels5g.size)
        for (ch in result.channels5g) {
            assertEquals(0, ch.apCount)
        }
    }

    @Test
    fun analyze_singleAp_on_channel1_shows_apCount_1() {
        val ap = buildAp(channel = 1, frequency = 2412, band = WifiBand.BAND_2_4_GHZ)
        val result = ChannelAnalyzer.analyze(listOf(ap))

        val ch1 = result.channels2g.first { it.channel == 1 }
        assertEquals(1, ch1.apCount)

        // Channel 6 should have 0 APs on it (co-channel)
        val ch6 = result.channels2g.first { it.channel == 6 }
        assertEquals(0, ch6.apCount)
    }

    @Test
    fun analyze_recommended_channel_avoids_congested_channel() {
        // Place strong APs on channels 1 and 6; channel 11 should be recommended
        val aps = listOf(
            buildAp(bssid = "AA:BB:CC:DD:EE:01", channel = 1, rssi = -30, frequency = 2412),
            buildAp(bssid = "AA:BB:CC:DD:EE:02", channel = 1, rssi = -35, frequency = 2412),
            buildAp(bssid = "AA:BB:CC:DD:EE:03", channel = 6, rssi = -30, frequency = 2437),
        )

        val result = ChannelAnalyzer.analyze(aps)
        // Channel 11 is the only non-overlapping channel with no APs
        assertEquals(11, result.recommended2g)
    }

    @Test
    fun analyze_5ghz_covers_expected_channels() {
        val ap = buildAp(
            channel = 36,
            frequency = 5180,
            band = WifiBand.BAND_5_GHZ,
        )
        val result = ChannelAnalyzer.analyze(listOf(ap))

        val channelNumbers = result.channels5g.map { it.channel }
        assertTrue(36 in channelNumbers)
        assertTrue(149 in channelNumbers)
        assertTrue(165 in channelNumbers)

        val ch36 = result.channels5g.first { it.channel == 36 }
        assertEquals(1, ch36.apCount)
    }

    @Test
    fun analyze_5ghz_empty_recommends_channel_with_lowest_congestion() {
        val result = ChannelAnalyzer.analyze(emptyList())
        // With no APs all 5 GHz channels have 0 congestion; minByOrNull picks first = 36
        assertTrue(result.recommended5g in result.channels5g.map { it.channel })
    }
}
