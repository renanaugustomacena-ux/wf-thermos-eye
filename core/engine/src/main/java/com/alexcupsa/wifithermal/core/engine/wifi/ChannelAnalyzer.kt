package com.alexcupsa.wifithermal.core.engine.wifi

import com.alexcupsa.wifithermal.core.model.ChannelAnalysisResult
import com.alexcupsa.wifithermal.core.model.ChannelInfo
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.WifiBand
import kotlin.math.abs
import kotlin.math.pow

/**
 * Analyses channel congestion from a list of scan results and recommends the
 * least-congested channel per band.
 *
 * Congestion scoring (0.0 = idle, 1.0 = saturated):
 *
 * 1. **Co-channel interference**: Each AP on the same channel contributes
 *    a weight proportional to its linear power (10^(RSSI/10)). This is
 *    normalised against a reference "strong AP" at -30 dBm so that a single
 *    strong AP yields ~1.0.
 *
 * 2. **Adjacent-channel interference (2.4 GHz only)**: In the 2.4 GHz band
 *    channels overlap by 22 MHz, so an AP on channel 1 bleeds into channels
 *    2-4 with decreasing attenuation. We apply a linear decay factor based
 *    on channel separation (0 dB at separation 0, attenuated ~25 dB at
 *    separation 5+).
 *
 * The final score is the sum of all weighted contributions, clamped to [0, 1].
 */
object ChannelAnalyzer {

    /** Reference power level (linear) for a very strong AP at -30 dBm. */
    private val REFERENCE_POWER = 10.0.pow(-30.0 / 10.0)

    /**
     * Analyse channel congestion and recommend the best channel per band.
     *
     * @param scanResults Processed scan results from the latest scan cycle.
     * @return [ChannelAnalysisResult] containing per-channel metrics and
     *         recommended channels for 2.4 GHz and 5 GHz.
     */
    fun analyze(scanResults: List<ProcessedScanResult>): ChannelAnalysisResult {
        val byBand = scanResults.groupBy { it.band }

        val results2g = byBand[WifiBand.BAND_2_4_GHZ].orEmpty()
        val results5g = byBand[WifiBand.BAND_5_GHZ].orEmpty()

        val channels2g = analyze2gChannels(results2g)
        val channels5g = analyze5gChannels(results5g)

        val recommended2g = recommendChannel2g(channels2g)
        val recommended5g = recommendChannel5g(channels5g)

        return ChannelAnalysisResult(
            channels2g = channels2g,
            channels5g = channels5g,
            recommended2g = recommended2g,
            recommended5g = recommended5g,
        )
    }

    // --- 2.4 GHz analysis ---------------------------------------------------

    private fun analyze2gChannels(
        results: List<ProcessedScanResult>,
    ): List<ChannelInfo> {
        val byChannel = results.groupBy { it.channel }

        return (1..13).map { ch ->
            val coChannel = byChannel[ch].orEmpty()
            val congestion = compute2gCongestion(ch, results)

            ChannelInfo(
                channel = ch,
                band = WifiBand.BAND_2_4_GHZ,
                frequency = ChannelMapper.channelToFrequency(ch, WifiBand.BAND_2_4_GHZ),
                apCount = coChannel.size,
                overlappingApCount = countOverlapping2g(ch, results),
                strongestSignal = coChannel.maxOfOrNull { it.rssi } ?: -100,
                averageSignal = coChannel.map { it.rssi.toDouble() }
                    .average().takeUnless { it.isNaN() } ?: -100.0,
                congestionScore = congestion,
            )
        }
    }

    /**
     * Compute 2.4 GHz congestion for [targetChannel] considering both
     * co-channel and adjacent-channel interference.
     *
     * Adjacent-channel attenuation model (simplified):
     *   separation 0 -> factor 1.0  (co-channel, full interference)
     *   separation 1 -> factor 0.7
     *   separation 2 -> factor 0.4
     *   separation 3 -> factor 0.2
     *   separation 4 -> factor 0.05
     *   separation 5+ -> factor 0.0  (non-overlapping)
     */
    private fun compute2gCongestion(
        targetChannel: Int,
        results: List<ProcessedScanResult>,
    ): Double {
        var totalInterference = 0.0

        for (ap in results) {
            val sep = abs(ap.channel - targetChannel)
            val overlapFactor = when (sep) {
                0 -> 1.0
                1 -> 0.7
                2 -> 0.4
                3 -> 0.2
                4 -> 0.05
                else -> 0.0
            }
            if (overlapFactor > 0.0) {
                val linearPower = 10.0.pow(ap.rssi.toDouble() / 10.0)
                totalInterference += overlapFactor * (linearPower / REFERENCE_POWER)
            }
        }

        return totalInterference.coerceIn(0.0, 1.0)
    }

    /** Count APs whose 22 MHz channel overlaps with [targetChannel]. */
    private fun countOverlapping2g(
        targetChannel: Int,
        results: List<ProcessedScanResult>,
    ): Int = results.count { ap ->
        val sep = abs(ap.channel - targetChannel)
        sep in 1..4 // channels within 4 apart overlap; 0 is co-channel, 5+ is clear
    }

    private fun recommendChannel2g(channels: List<ChannelInfo>): Int {
        // Prefer non-overlapping channels (1, 6, 11) to avoid adjacent
        // interference with other well-configured networks.
        val nonOverlapping = channels.filter { it.channel in ChannelMapper.NON_OVERLAPPING_2_4 }
        return nonOverlapping.minByOrNull { it.congestionScore }?.channel
            ?: channels.minByOrNull { it.congestionScore }?.channel
            ?: 1
    }

    // --- 5 GHz analysis ------------------------------------------------------

    /** Standard 5 GHz 20 MHz channels (UNII-1 through UNII-3). */
    private val CHANNELS_5G = listOf(
        36, 40, 44, 48,         // UNII-1
        52, 56, 60, 64,         // UNII-2
        100, 104, 108, 112,     // UNII-2 Extended
        116, 120, 124, 128,
        132, 136, 140, 144,
        149, 153, 157, 161, 165 // UNII-3
    )

    private fun analyze5gChannels(
        results: List<ProcessedScanResult>,
    ): List<ChannelInfo> {
        val byChannel = results.groupBy { it.channel }

        return CHANNELS_5G.map { ch ->
            val coChannel = byChannel[ch].orEmpty()
            val congestion = compute5gCongestion(coChannel)

            ChannelInfo(
                channel = ch,
                band = WifiBand.BAND_5_GHZ,
                frequency = ChannelMapper.channelToFrequency(ch, WifiBand.BAND_5_GHZ),
                apCount = coChannel.size,
                overlappingApCount = 0, // 5 GHz channels do not overlap at 20 MHz
                strongestSignal = coChannel.maxOfOrNull { it.rssi } ?: -100,
                averageSignal = coChannel.map { it.rssi.toDouble() }
                    .average().takeUnless { it.isNaN() } ?: -100.0,
                congestionScore = congestion,
            )
        }
    }

    /**
     * 5 GHz congestion is purely co-channel (no overlap at 20 MHz spacing).
     */
    private fun compute5gCongestion(coChannelAps: List<ProcessedScanResult>): Double {
        if (coChannelAps.isEmpty()) return 0.0

        var totalInterference = 0.0
        for (ap in coChannelAps) {
            totalInterference += 10.0.pow(ap.rssi.toDouble() / 10.0) / REFERENCE_POWER
        }
        return totalInterference.coerceIn(0.0, 1.0)
    }

    private fun recommendChannel5g(channels: List<ChannelInfo>): Int {
        return channels.minByOrNull { it.congestionScore }?.channel ?: 36
    }
}
