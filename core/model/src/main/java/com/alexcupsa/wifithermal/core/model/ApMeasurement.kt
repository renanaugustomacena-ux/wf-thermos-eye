package com.alexcupsa.wifithermal.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApMeasurement(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val smoothedRssi: Double,
    val frequency: Int,
    val channel: Int,
    val channelWidth: ChannelWidth,
    val band: WifiBand,
    val security: SecurityType,
    val standard: WifiStandard = WifiStandard.LEGACY,
)
