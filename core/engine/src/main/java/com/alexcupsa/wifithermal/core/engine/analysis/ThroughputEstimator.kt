package com.alexcupsa.wifithermal.core.engine.analysis

import com.alexcupsa.wifithermal.core.model.ChannelWidth
import com.alexcupsa.wifithermal.core.model.WifiStandard
import kotlin.math.log2
import kotlin.math.pow

/**
 * Throughput estimation from RSSI, WiFi standard, and channel width.
 *
 * Uses the Shannon-Hartley theorem as a theoretical upper bound and applies
 * empirical efficiency factors to produce a realistic "estimated actual"
 * throughput that accounts for protocol overhead, contention, and
 * environmental degradation.
 *
 * The model:
 *
 * 1. **Max theoretical** = max PHY rate for the (standard, channelWidth) tuple,
 *    scaled by the number of spatial streams (assumed from the standard).
 *
 * 2. **SNR-based capacity** = bandwidth * log2(1 + SNR_linear), where
 *    SNR = RSSI - noise_floor.
 *
 * 3. **Estimated actual** = min(max_theoretical, snr_capacity) * efficiency,
 *    where efficiency accounts for MAC overhead, contention, and TCP/IP
 *    encapsulation (typically 0.5-0.7 for real-world conditions).
 *
 * 4. **Confidence** [0.0-1.0] reflects how reliable the estimate is, based
 *    on signal quality. Strong signals yield high confidence.
 */
object ThroughputEstimator {

    /** Default noise floor assumption in dBm (typical for 2.4/5 GHz). */
    private const val NOISE_FLOOR_DBM = -95.0

    /**
     * Result of a throughput estimation.
     *
     * @property maxTheoreticalMbps Maximum PHY-layer data rate in Mbps.
     * @property estimatedActualMbps Estimated real-world throughput in Mbps.
     * @property confidence Confidence level [0.0, 1.0] of the estimate.
     */
    data class ThroughputEstimate(
        val maxTheoreticalMbps: Double,
        val estimatedActualMbps: Double,
        val confidence: Double,
    )

    /**
     * Estimate throughput for a given signal condition.
     *
     * @param rssi          Smoothed RSSI in dBm (e.g. -55.0).
     * @param standard      WiFi standard (LEGACY / WIFI_4_N / WIFI_5_AC).
     * @param channelWidth  Channel width (20/40/80/160 MHz).
     * @param noiseFloorDbm Noise floor in dBm (default -95).
     * @return [ThroughputEstimate] with theoretical max, estimated actual,
     *         and confidence.
     */
    fun estimate(
        rssi: Double,
        standard: WifiStandard,
        channelWidth: ChannelWidth,
        noiseFloorDbm: Double = NOISE_FLOOR_DBM,
    ): ThroughputEstimate {
        val maxTheoretical = maxPhyRate(standard, channelWidth)
        val bandwidthMhz = channelWidthMhz(channelWidth)

        // SNR in dB; clamp to a minimum of 0 (at or below noise floor)
        val snrDb = (rssi - noiseFloorDbm).coerceAtLeast(0.0)
        val snrLinear = 10.0.pow(snrDb / 10.0)

        // Shannon capacity (Mbps)
        val shannonCapacity = bandwidthMhz * log2(1.0 + snrLinear)

        // Efficiency factor: MAC overhead (~30%), contention (~10-20%),
        // TCP/IP overhead (~5%). Combined ~50-65% of PHY rate.
        val efficiency = efficiencyFactor(standard, snrDb)

        val estimatedActual = minOf(maxTheoretical, shannonCapacity) * efficiency

        val confidence = computeConfidence(rssi)

        return ThroughputEstimate(
            maxTheoreticalMbps = maxTheoretical,
            estimatedActualMbps = estimatedActual.coerceAtLeast(0.0),
            confidence = confidence,
        )
    }

    // -------------------------------------------------------------------------
    // Max PHY rates (single user, common spatial stream counts)
    // -------------------------------------------------------------------------

    /**
     * Maximum PHY data rate in Mbps for the given standard and channel width.
     *
     * Assumes:
     * - LEGACY (802.11a/b/g): 1 spatial stream, 20 MHz only.
     * - WIFI_4_N (802.11n): 2 spatial streams (2x2 MIMO).
     * - WIFI_5_AC (802.11ac): 2 spatial streams (2x2 MIMO), 256-QAM.
     * - WIFI_6_AX (802.11ax): 2 spatial streams (2x2 MIMO), 1024-QAM.
     * - WIFI_7_BE (802.11be): 2 spatial streams (2x2 MIMO), 4096-QAM.
     *
     * These are maximum MCS rates from the respective standards.
     */
    private fun maxPhyRate(standard: WifiStandard, width: ChannelWidth): Double =
        when (standard) {
            WifiStandard.LEGACY -> 54.0
            WifiStandard.WIFI_4_N -> when (width) {
                ChannelWidth.MHZ_20 -> 144.4
                ChannelWidth.MHZ_40 -> 300.0
                else -> 300.0
            }
            WifiStandard.WIFI_5_AC -> when (width) {
                ChannelWidth.MHZ_20 -> 173.3
                ChannelWidth.MHZ_40 -> 400.0
                ChannelWidth.MHZ_80 -> 866.7
                ChannelWidth.MHZ_160 -> 1733.0
                else -> 1733.0
            }
            WifiStandard.WIFI_6_AX -> when (width) {
                ChannelWidth.MHZ_20 -> 286.8
                ChannelWidth.MHZ_40 -> 573.5
                ChannelWidth.MHZ_80 -> 1201.0
                ChannelWidth.MHZ_160 -> 2402.0
                else -> 2402.0
            }
            WifiStandard.WIFI_7_BE -> when (width) {
                ChannelWidth.MHZ_20 -> 573.5
                ChannelWidth.MHZ_40 -> 1147.0
                ChannelWidth.MHZ_80 -> 2402.0
                ChannelWidth.MHZ_160 -> 4804.0
                ChannelWidth.MHZ_320 -> 9608.0
            }
        }

    private fun channelWidthMhz(width: ChannelWidth): Double = when (width) {
        ChannelWidth.MHZ_20 -> 20.0
        ChannelWidth.MHZ_40 -> 40.0
        ChannelWidth.MHZ_80 -> 80.0
        ChannelWidth.MHZ_160 -> 160.0
        ChannelWidth.MHZ_320 -> 320.0
    }

    /**
     * Empirical efficiency factor that scales the theoretical rate to an
     * estimated real-world throughput.
     *
     * Higher SNR and newer standards achieve better efficiency because:
     * - Higher MCS indices pack more data per symbol.
     * - Block ACK and frame aggregation in 802.11n/ac reduce overhead.
     * - Good SNR avoids retransmissions.
     */
    private fun efficiencyFactor(standard: WifiStandard, snrDb: Double): Double {
        val baseEfficiency = when (standard) {
            WifiStandard.LEGACY -> 0.45
            WifiStandard.WIFI_4_N -> 0.55
            WifiStandard.WIFI_5_AC -> 0.60
            WifiStandard.WIFI_6_AX -> 0.65
            WifiStandard.WIFI_7_BE -> 0.68
        }

        // SNR penalty: at low SNR, retransmissions and rate fallback degrade
        // efficiency further. The penalty ramps from 0 at SNR=30 dB to 0.3
        // at SNR=0 dB.
        val snrPenalty = ((30.0 - snrDb.coerceAtMost(30.0)) / 30.0) * 0.3

        return (baseEfficiency - snrPenalty).coerceIn(0.1, 0.75)
    }

    /**
     * Confidence [0.0, 1.0] based on signal quality.
     *
     * Strong signals produce predictable throughput; very weak signals make
     * estimation unreliable due to stochastic fading and rate oscillation.
     */
    private fun computeConfidence(rssi: Double): Double = when {
        rssi >= -50.0 -> 0.95
        rssi >= -60.0 -> 0.85
        rssi >= -70.0 -> 0.70
        rssi >= -80.0 -> 0.50
        rssi >= -85.0 -> 0.30
        else -> 0.15
    }
}
