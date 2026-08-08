package com.alexcupsa.wifithermal.core.data.adapter

import android.net.wifi.ScanResult
import android.os.SystemClock
import com.alexcupsa.wifithermal.core.engine.signal.PathLossModel
import com.alexcupsa.wifithermal.core.engine.signal.SignalProcessor
import com.alexcupsa.wifithermal.core.engine.wifi.ChannelMapper
import com.alexcupsa.wifithermal.core.engine.wifi.OuiLookup
import com.alexcupsa.wifithermal.core.engine.wifi.SecurityParser
import com.alexcupsa.wifithermal.core.model.ChannelWidth
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.model.WifiStandard
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiScanAdapter @Inject constructor(
    private val signalProcessor: SignalProcessor,
) {
    @Suppress("DEPRECATION") // ScanResult.SSID: minSdk=34 still requires the
    // string field; the new getWifiSsid() returns a WifiSsid object that round-
    // trips to the same value with extra ceremony. Stick with SSID for now.
    fun process(
        scanResults: List<ScanResult>,
        connectedBssid: String?,
    ): List<ProcessedScanResult> {
        // Sample one anchor for elapsed-real-time so every result in this batch
        // gets a consistent age relative to the same now.
        val nowUs = SystemClock.elapsedRealtimeNanos() / 1000L
        return scanResults.map { sr ->
            val smoothed = signalProcessor.smooth(sr.BSSID, sr.level)
            val band = ChannelMapper.bandFromFrequency(sr.frequency)
            val channel = ChannelMapper.frequencyToChannel(sr.frequency)
            val security = SecurityParser.parse(sr.capabilities)
            val channelWidth = mapChannelWidth(sr.channelWidth)
            val standard = mapWifiStandard(sr.wifiStandard)
            val distance = PathLossModel.estimateDistance(smoothed, sr.frequency)

            // ScanResult.timestamp is microseconds since boot; convert age to ms.
            val ageMs = ((nowUs - sr.timestamp) / 1000L).coerceAtLeast(0L)

            ProcessedScanResult(
                ssid = sr.SSID ?: "",
                bssid = sr.BSSID,
                rssi = sr.level,
                smoothedRssi = smoothed,
                frequency = sr.frequency,
                channel = channel,
                channelWidth = channelWidth,
                band = band,
                security = security,
                standard = standard,
                signalQuality = SignalQuality.fromRssi(smoothed),
                estimatedDistance = distance,
                isConnected = sr.BSSID == connectedBssid,
                vendor = OuiLookup.lookup(sr.BSSID),
                ageMs = ageMs,
            )
        }
    }

    private fun mapChannelWidth(width: Int): ChannelWidth = when (width) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> ChannelWidth.MHZ_20
        ScanResult.CHANNEL_WIDTH_40MHZ -> ChannelWidth.MHZ_40
        ScanResult.CHANNEL_WIDTH_80MHZ -> ChannelWidth.MHZ_80
        ScanResult.CHANNEL_WIDTH_160MHZ -> ChannelWidth.MHZ_160
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> ChannelWidth.MHZ_160
        ScanResult.CHANNEL_WIDTH_320MHZ -> ChannelWidth.MHZ_320
        else -> ChannelWidth.MHZ_20
    }

    private fun mapWifiStandard(standard: Int): WifiStandard = when (standard) {
        ScanResult.WIFI_STANDARD_11N -> WifiStandard.WIFI_4_N
        ScanResult.WIFI_STANDARD_11AC -> WifiStandard.WIFI_5_AC
        ScanResult.WIFI_STANDARD_11AX -> WifiStandard.WIFI_6_AX
        ScanResult.WIFI_STANDARD_11BE -> WifiStandard.WIFI_7_BE
        else -> WifiStandard.LEGACY
    }
}
