package com.alexcupsa.wifithermal.core.model.audit

import kotlinx.serialization.Serializable

/**
 * One observation of a BSSID at a known device location. Accumulated rows
 * with the same [bssid] become input to [com.alexcupsa.wifithermal.core.engine.audit.Triangulator].
 */
@Serializable
data class RssiSample(
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val timestamp: Long,
)

/**
 * Output of triangulation. Confidence is in [0, 1] and combines:
 * - Sample count (more = better, saturating around 10)
 * - Geographic spread of sample positions (wider baseline = better)
 * - Average device-side GPS accuracy
 *
 * Coordinates are WGS84 (lat, lon) for direct map display.
 */
@Serializable
data class TriangulationResult(
    val bssid: String,
    val estimatedLat: Double,
    val estimatedLon: Double,
    val confidence: Double,
    val sampleCount: Int,
    val baselineMetres: Double,
)
