package com.alexcupsa.wifithermal.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class WifiBand { BAND_2_4_GHZ, BAND_5_GHZ }

@Serializable
enum class ChannelWidth { MHZ_20, MHZ_40, MHZ_80, MHZ_160, MHZ_320 }

@Serializable
enum class SecurityType { OPEN, WEP, WPA_PSK, WPA2_PSK, WPA3_SAE, WPA_EAP, WPA2_EAP, UNKNOWN }

@Serializable
enum class WifiStandard { LEGACY, WIFI_4_N, WIFI_5_AC, WIFI_6_AX, WIFI_7_BE }

@Serializable
enum class SurveyStatus { ACTIVE, COMPLETED, ARCHIVED }

@Serializable
enum class InterpolationAlgorithm { IDW, KRIGING, RBF, NEAREST }

@Serializable
enum class ColorScheme { THERMAL, VIRIDIS, MAGMA, GRAYSCALE }

enum class SignalQuality(val label: String, val minRssi: Int, val colorArgb: Long) {
    EXCELLENT("Excellent", -50, 0xFF00C853),
    GOOD("Good", -60, 0xFF64DD17),
    FAIR("Fair", -70, 0xFFFFD600),
    WEAK("Weak", -80, 0xFFFF6D00),
    UNUSABLE("Unusable", -100, 0xFFD50000);

    companion object {
        fun fromRssi(rssi: Int): SignalQuality = entries.firstOrNull { rssi >= it.minRssi } ?: UNUSABLE
        fun fromRssi(rssi: Double): SignalQuality = fromRssi(rssi.toInt())
    }
}

enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
enum class SecurityScore { EXCELLENT, GOOD, FAIR, POOR, CRITICAL }

enum class ScanStatus {
    IDLE, SCANNING, THROTTLED, COMPLETE
}
