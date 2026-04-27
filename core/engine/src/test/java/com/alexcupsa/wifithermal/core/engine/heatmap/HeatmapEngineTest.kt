package com.alexcupsa.wifithermal.core.engine.heatmap

import com.alexcupsa.wifithermal.core.engine.analysis.ThroughputEstimator
import com.alexcupsa.wifithermal.core.engine.security.SecurityAuditor
import com.alexcupsa.wifithermal.core.model.ChannelWidth
import com.alexcupsa.wifithermal.core.model.ColorScheme
import com.alexcupsa.wifithermal.core.model.HeatmapConfig
import com.alexcupsa.wifithermal.core.model.MeasurementSample
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SecurityScore
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.Severity
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.model.WifiBand
import com.alexcupsa.wifithermal.core.model.WifiStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for heatmap generation, colour mapping, security auditing, and
 * throughput estimation engine objects.
 *
 * Covers: [IdwInterpolator], [HeatmapEngine], [HeatmapColors],
 * [SecurityAuditor], and [ThroughputEstimator].
 */
class HeatmapEngineTest {

    // =========================================================================
    // Helper
    // =========================================================================

    private fun buildAp(
        ssid: String = "Test",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        rssi: Int = -50,
        security: SecurityType = SecurityType.WPA2_PSK,
        vendor: String? = null,
    ): ProcessedScanResult = ProcessedScanResult(
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
        signalQuality = SignalQuality.EXCELLENT,
        estimatedDistance = 3.0,
        vendor = vendor,
    )

    // =========================================================================
    // IdwInterpolator
    // =========================================================================

    @Test
    fun idw_exactMatch_returns_sample_value() {
        val samples = listOf(MeasurementSample(x = 0.0, y = 0.0, rssi = -45.0))
        val grid = IdwInterpolator.interpolate(samples, width = 1, height = 1)
        assertEquals(-45.0, grid[0], 0.1)
    }

    @Test
    fun idw_two_samples_equidistant_returns_average() {
        // Grid cell (1, 0) is equidistant from (0, 0) and (2, 0)
        val samples = listOf(
            MeasurementSample(x = 0.0, y = 0.0, rssi = -40.0),
            MeasurementSample(x = 2.0, y = 0.0, rssi = -60.0),
        )
        val grid = IdwInterpolator.interpolate(samples, width = 3, height = 1)
        // Cell at col=1, row=0 -> index 1
        assertEquals(-50.0, grid[1], 0.1)
    }

    @Test
    fun idw_single_sample_near_grid_center() {
        // 3x3 grid, sample at centre (1.0, 1.0)
        val samples = listOf(MeasurementSample(x = 1.0, y = 1.0, rssi = -55.0))
        val grid = IdwInterpolator.interpolate(samples, width = 3, height = 3)
        // Centre cell (col=1, row=1) -> index 4 is an exact match
        assertEquals(-55.0, grid[4], 0.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun idw_empty_samples_throws() {
        IdwInterpolator.interpolate(emptyList(), width = 5, height = 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun idw_zero_width_throws() {
        val samples = listOf(MeasurementSample(x = 0.0, y = 0.0, rssi = -50.0))
        IdwInterpolator.interpolate(samples, width = 0, height = 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun idw_zero_height_throws() {
        val samples = listOf(MeasurementSample(x = 0.0, y = 0.0, rssi = -50.0))
        IdwInterpolator.interpolate(samples, width = 5, height = 0)
    }

    // =========================================================================
    // HeatmapEngine
    // =========================================================================

    @Test(expected = IllegalArgumentException::class)
    fun heatmapEngine_fewer_than_3_samples_throws() {
        val samples = listOf(
            MeasurementSample(x = 0.0, y = 0.0, rssi = -50.0),
            MeasurementSample(x = 1.0, y = 1.0, rssi = -60.0),
        )
        HeatmapEngine.generate(samples, width = 10, height = 10)
    }

    @Test
    fun heatmapEngine_valid_3_samples_produces_correct_dimensions() {
        val samples = listOf(
            MeasurementSample(x = 0.0, y = 0.0, rssi = -40.0),
            MeasurementSample(x = 9.0, y = 0.0, rssi = -60.0),
            MeasurementSample(x = 4.0, y = 9.0, rssi = -50.0),
        )
        val grid = HeatmapEngine.generate(samples, width = 10, height = 10)
        assertEquals(10, grid.width)
        assertEquals(10, grid.height)
        assertEquals(100, grid.data.size)
    }

    @Test
    fun heatmapEngine_minValue_leq_maxValue() {
        val samples = listOf(
            MeasurementSample(x = 0.0, y = 0.0, rssi = -40.0),
            MeasurementSample(x = 4.0, y = 0.0, rssi = -70.0),
            MeasurementSample(x = 2.0, y = 4.0, rssi = -55.0),
        )
        val grid = HeatmapEngine.generate(samples, width = 5, height = 5)
        assertTrue(
            "minValue (${grid.minValue}) should be <= maxValue (${grid.maxValue})",
            grid.minValue <= grid.maxValue,
        )
    }

    @Test
    fun heatmapEngine_no_nan_in_center_cells() {
        val samples = listOf(
            MeasurementSample(x = 0.0, y = 0.0, rssi = -40.0),
            MeasurementSample(x = 4.0, y = 0.0, rssi = -60.0),
            MeasurementSample(x = 2.0, y = 4.0, rssi = -50.0),
        )
        val grid = HeatmapEngine.generate(samples, width = 5, height = 5)
        // Centre cell at (2, 2) -> index 12
        assertFalse(
            "Centre cell should not be NaN",
            grid.data[12].isNaN(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun heatmapEngine_zero_width_throws() {
        val samples = listOf(
            MeasurementSample(x = 0.0, y = 0.0, rssi = -40.0),
            MeasurementSample(x = 1.0, y = 0.0, rssi = -50.0),
            MeasurementSample(x = 0.0, y = 1.0, rssi = -60.0),
        )
        HeatmapEngine.generate(samples, width = 0, height = 10)
    }

    @Test
    fun heatmapEngine_smoothingRadius_zero_skips_blur() {
        val samples = listOf(
            MeasurementSample(x = 0.0, y = 0.0, rssi = -40.0),
            MeasurementSample(x = 4.0, y = 0.0, rssi = -60.0),
            MeasurementSample(x = 2.0, y = 4.0, rssi = -50.0),
        )
        val config = HeatmapConfig(smoothingRadius = 0)
        val grid = HeatmapEngine.generate(samples, width = 5, height = 5, config = config)
        // Exact sample locations should match their RSSI precisely (no blur)
        assertEquals(-40.0, grid.data[0], 0.1) // (0,0) -> index 0
    }

    @Test
    fun heatmapConfig_default_constructor_works() {
        val config = HeatmapConfig()
        // Just verifying it does not throw and has sane defaults
        assertTrue(config.smoothingRadius >= 0)
        assertTrue(config.idwPower > 0.0)
    }

    // =========================================================================
    // HeatmapColors
    // =========================================================================

    @Test
    fun colors_strong_signal_at_maxRssi_maps_to_index_0() {
        // rssi == maxRssi means t = 0.0, index = 0
        val color = HeatmapColors.rssiToColor(
            rssi = -30.0, scheme = ColorScheme.THERMAL,
            minRssi = -90.0, maxRssi = -30.0,
        )
        // Index 0 in THERMAL is red (255, 0, 0). Check red channel.
        val r = (color shr 16) and 0xFF
        assertEquals(255, r)
    }

    @Test
    fun colors_weak_signal_at_minRssi_maps_to_index_255() {
        // rssi == minRssi means t = 1.0, index = 255
        val color = HeatmapColors.rssiToColor(
            rssi = -90.0, scheme = ColorScheme.THERMAL,
            minRssi = -90.0, maxRssi = -30.0,
        )
        // Index 255 in THERMAL is blue (0, 0, 200). Check blue channel.
        val b = color and 0xFF
        assertEquals(200, b)
    }

    @Test
    fun colors_alpha_is_applied() {
        val color = HeatmapColors.rssiToColor(
            rssi = -60.0, scheme = ColorScheme.THERMAL, alpha = 128,
        )
        val alpha = (color ushr 24) and 0xFF
        assertEquals(128, alpha)
    }

    @Test
    fun colors_different_schemes_produce_different_colors() {
        val thermal = HeatmapColors.rssiToColor(-60.0, ColorScheme.THERMAL)
        val viridis = HeatmapColors.rssiToColor(-60.0, ColorScheme.VIRIDIS)
        assertNotEquals(
            "THERMAL and VIRIDIS should produce different colours for the same RSSI",
            thermal, viridis,
        )
    }

    @Test
    fun colors_equal_min_max_rssi_returns_middle_color() {
        // When minRssi == maxRssi, the code returns t = 0.5, index = 127
        val color = HeatmapColors.rssiToColor(
            rssi = -60.0, scheme = ColorScheme.GRAYSCALE,
            minRssi = -60.0, maxRssi = -60.0,
        )
        // GRAYSCALE at index 127: v = 255 - 127 = 128 -> rgb(128,128,128)
        val r = (color shr 16) and 0xFF
        assertEquals(128, r)
    }

    // =========================================================================
    // SecurityAuditor
    // =========================================================================

    @Test
    fun audit_empty_scan_returns_excellent_score() {
        val result = SecurityAuditor.audit(emptyList())
        assertEquals(SecurityScore.EXCELLENT, result.overallScore)
        assertEquals(0, result.totalAps)
    }

    @Test
    fun audit_all_wpa3_returns_excellent_score() {
        val aps = listOf(
            buildAp(bssid = "AA:BB:CC:DD:EE:01", security = SecurityType.WPA3_SAE),
            buildAp(bssid = "AA:BB:CC:DD:EE:02", security = SecurityType.WPA3_SAE),
        )
        val result = SecurityAuditor.audit(aps)
        assertEquals(SecurityScore.EXCELLENT, result.overallScore)
        assertEquals(2, result.wpa3Count)
    }

    @Test
    fun audit_open_network_produces_critical_finding() {
        val aps = listOf(buildAp(security = SecurityType.OPEN))
        val result = SecurityAuditor.audit(aps)
        assertEquals(1, result.openCount)
        assertTrue(
            "Should contain a CRITICAL finding for open network",
            result.findings.any { it.severity == Severity.CRITICAL },
        )
    }

    @Test
    fun audit_wep_network_produces_critical_finding() {
        val aps = listOf(buildAp(security = SecurityType.WEP))
        val result = SecurityAuditor.audit(aps)
        assertEquals(1, result.wepCount)
        assertTrue(
            "Should contain a CRITICAL finding for WEP",
            result.findings.any {
                it.severity == Severity.CRITICAL && it.title.contains("WEP")
            },
        )
    }

    @Test
    fun audit_hidden_ssid_produces_medium_finding() {
        val aps = listOf(buildAp(ssid = ""))
        val result = SecurityAuditor.audit(aps)
        assertTrue(
            "Should contain a MEDIUM finding for hidden SSID",
            result.findings.any {
                it.severity == Severity.MEDIUM && it.title.contains("Hidden")
            },
        )
    }

    @Test
    fun audit_default_ssid_produces_medium_finding() {
        val aps = listOf(buildAp(ssid = "linksys"))
        val result = SecurityAuditor.audit(aps)
        assertTrue(
            "Should contain a MEDIUM finding for default SSID",
            result.findings.any {
                it.severity == Severity.MEDIUM && it.title.contains("Default")
            },
        )
    }

    @Test
    fun audit_mixed_security_counts_correct() {
        val aps = listOf(
            buildAp(bssid = "AA:BB:CC:DD:EE:01", security = SecurityType.OPEN),
            buildAp(bssid = "AA:BB:CC:DD:EE:02", security = SecurityType.WEP),
            buildAp(bssid = "AA:BB:CC:DD:EE:03", security = SecurityType.WPA_PSK),
            buildAp(bssid = "AA:BB:CC:DD:EE:04", security = SecurityType.WPA2_PSK),
            buildAp(bssid = "AA:BB:CC:DD:EE:05", security = SecurityType.WPA3_SAE),
        )
        val result = SecurityAuditor.audit(aps)
        assertEquals(5, result.totalAps)
        assertEquals(1, result.openCount)
        assertEquals(1, result.wepCount)
        assertEquals(1, result.wpaCount)
        assertEquals(1, result.wpa2Count)
        assertEquals(1, result.wpa3Count)
    }

    // =========================================================================
    // ThroughputEstimator
    // =========================================================================

    @Test
    fun throughput_wifi5_ac_80mhz_at_minus50_max_theoretical() {
        val est = ThroughputEstimator.estimate(
            rssi = -50.0,
            standard = WifiStandard.WIFI_5_AC,
            channelWidth = ChannelWidth.MHZ_80,
        )
        assertEquals(866.7, est.maxTheoreticalMbps, 0.1)
    }

    @Test
    fun throughput_wifi6_ax_higher_than_wifi5_ac() {
        val ac = ThroughputEstimator.estimate(-50.0, WifiStandard.WIFI_5_AC, ChannelWidth.MHZ_80)
        val ax = ThroughputEstimator.estimate(-50.0, WifiStandard.WIFI_6_AX, ChannelWidth.MHZ_80)
        assertTrue(
            "WiFi 6 AX max theoretical (${ax.maxTheoreticalMbps}) > WiFi 5 AC (${ac.maxTheoreticalMbps})",
            ax.maxTheoreticalMbps > ac.maxTheoreticalMbps,
        )
    }

    @Test
    fun throughput_weaker_signal_lower_estimated_actual() {
        val strong = ThroughputEstimator.estimate(-40.0, WifiStandard.WIFI_5_AC, ChannelWidth.MHZ_80)
        val weak = ThroughputEstimator.estimate(-80.0, WifiStandard.WIFI_5_AC, ChannelWidth.MHZ_80)
        assertTrue(
            "Strong signal actual (${strong.estimatedActualMbps}) > weak (${weak.estimatedActualMbps})",
            strong.estimatedActualMbps > weak.estimatedActualMbps,
        )
    }

    @Test
    fun throughput_confidence_decreases_with_weaker_signal() {
        val strong = ThroughputEstimator.estimate(-40.0, WifiStandard.WIFI_5_AC, ChannelWidth.MHZ_80)
        val weak = ThroughputEstimator.estimate(-85.0, WifiStandard.WIFI_5_AC, ChannelWidth.MHZ_80)
        assertTrue(
            "Strong confidence (${strong.confidence}) > weak (${weak.confidence})",
            strong.confidence > weak.confidence,
        )
    }

    @Test
    fun throughput_noise_floor_at_rssi_near_zero() {
        // RSSI == noise floor => SNR = 0 dB => Shannon capacity is tiny
        val est = ThroughputEstimator.estimate(
            rssi = -95.0,
            standard = WifiStandard.WIFI_5_AC,
            channelWidth = ChannelWidth.MHZ_80,
            noiseFloorDbm = -95.0,
        )
        assertTrue(
            "At noise floor, estimated actual should be low, was ${est.estimatedActualMbps}",
            est.estimatedActualMbps < 30.0,
        )
    }

    @Test
    fun throughput_wifi7_be_320mhz_max_theoretical() {
        val est = ThroughputEstimator.estimate(
            rssi = -50.0,
            standard = WifiStandard.WIFI_7_BE,
            channelWidth = ChannelWidth.MHZ_320,
        )
        assertEquals(9608.0, est.maxTheoreticalMbps, 0.1)
    }
}
