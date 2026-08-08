package com.alexcupsa.wifithermal.core.engine.wifi

import com.alexcupsa.wifithermal.core.model.WifiBand

/**
 * Bidirectional mapping between WiFi channel numbers and centre frequencies.
 *
 * Covers 2.4 GHz (channels 1-14), 5 GHz UNII bands (channels 32-177),
 * and 6 GHz (channels 1-233, Wi-Fi 6E). Channel 14 (2484 MHz, Japan-only)
 * is handled as a special case because it does not follow the 5 MHz spacing
 * of channels 1-13.
 *
 * Reference: IEEE 802.11-2020, Annex E.
 */
object ChannelMapper {

    /** Non-overlapping channels for 2.4 GHz with 20 MHz width. */
    val NON_OVERLAPPING_2_4 = listOf(1, 6, 11)

    /**
     * Convert a centre frequency in MHz to a channel number.
     *
     * Returns -1 for unrecognised frequencies.
     */
    fun frequencyToChannel(freqMhz: Int): Int = when {
        freqMhz == 2484 -> 14
        freqMhz in 2412..2472 -> (freqMhz - 2407) / 5
        freqMhz in 5160..5885 -> (freqMhz - 5000) / 5
        freqMhz in 5955..7115 -> (freqMhz - 5950) / 5
        else -> -1
    }

    /**
     * Convert a channel number and [band] to a centre frequency in MHz.
     *
     * @throws IllegalArgumentException if [band] is not supported by this mapper.
     */
    fun channelToFrequency(channel: Int, band: WifiBand): Int = when (band) {
        WifiBand.BAND_2_4_GHZ -> if (channel == 14) 2484 else 2407 + channel * 5
        WifiBand.BAND_5_GHZ -> 5000 + channel * 5
    }

    /**
     * Determine the band from a centre frequency.
     *
     * Simple heuristic: anything below 5000 MHz is 2.4 GHz; 5000+ is 5 GHz.
     */
    fun bandFromFrequency(freqMhz: Int): WifiBand =
        if (freqMhz < 5000) WifiBand.BAND_2_4_GHZ else WifiBand.BAND_5_GHZ
}
