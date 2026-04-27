package com.alexcupsa.wifithermal.core.model

data class ProcessedScanResult(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val smoothedRssi: Double,
    val frequency: Int,
    val channel: Int,
    val channelWidth: ChannelWidth,
    val band: WifiBand,
    val security: SecurityType,
    val standard: WifiStandard,
    val signalQuality: SignalQuality,
    val estimatedDistance: Double,
    val isConnected: Boolean = false,
    val vendor: String? = null,
    val ageMs: Long = 0,
)
