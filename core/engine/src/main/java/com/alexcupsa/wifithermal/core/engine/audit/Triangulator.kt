package com.alexcupsa.wifithermal.core.engine.audit

import com.alexcupsa.wifithermal.core.engine.signal.PathLossModel
import com.alexcupsa.wifithermal.core.model.audit.RssiSample
import com.alexcupsa.wifithermal.core.model.audit.TriangulationResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Estimates the geographic position of an AP from a set of RSSI samples
 * captured at known device positions.
 *
 * Two stage computation:
 * 1. Convert each (lat, lon) to a local equirectangular x/y frame anchored
 *    at the centroid of the input samples — accurate to <1% over <1 km.
 * 2. Compute a weighted centroid where each sample is weighted by an
 *    inverse-distance estimate from RSSI via [PathLossModel].
 *
 * This is intentionally NOT a full least-squares trilateration — the latter
 * needs well-conditioned circle geometry which RSSI noise routinely breaks.
 * Weighted centroid is robust against bad samples and gives "where the AP
 * is closer to" within the convex hull of observation points, which is
 * what the operator actually wants in the field ("walk this way").
 *
 * Returns null if fewer than [MIN_SAMPLES] samples are provided. Confidence
 * scales with sample count (saturating ~10) and geographic spread
 * (baseline). Confidence below ~0.3 should be treated as a coarse hint.
 */
object Triangulator {

    const val MIN_SAMPLES = 3
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun triangulate(samples: List<RssiSample>): TriangulationResult? {
        if (samples.size < MIN_SAMPLES) return null
        val bssid = samples.first().bssid
        require(samples.all { it.bssid == bssid }) {
            "All samples must share the same BSSID, got mixed input"
        }

        // Reference centroid for the local frame — pick mean lat/lon.
        val refLat = samples.sumOf { it.lat } / samples.size
        val refLon = samples.sumOf { it.lon } / samples.size
        val cosRefLat = cos(refLat * PI / 180.0)

        // Project each sample to local meters frame.
        data class LocalSample(val xM: Double, val yM: Double, val rssi: Int, val freqMhz: Int, val accuracy: Float)
        val locals = samples.map {
            LocalSample(
                xM = (it.lon - refLon) * cosRefLat * EARTH_RADIUS_M * PI / 180.0,
                yM = (it.lat - refLat) * EARTH_RADIUS_M * PI / 180.0,
                rssi = it.rssi,
                freqMhz = it.frequencyMhz,
                accuracy = it.accuracyM,
            )
        }

        // Weight = 1 / max(0.5, estimated_distance). Closer samples carry more.
        var sumWX = 0.0
        var sumWY = 0.0
        var sumW = 0.0
        for (s in locals) {
            val dist = PathLossModel.estimateDistance(s.rssi.toDouble(), s.freqMhz)
            val w = 1.0 / max(0.5, dist)
            sumWX += w * s.xM
            sumWY += w * s.yM
            sumW += w
        }
        if (sumW <= 0.0) return null

        val estXm = sumWX / sumW
        val estYm = sumWY / sumW

        // Convert local meters back to lat/lon.
        val estLat = refLat + (estYm / EARTH_RADIUS_M) * 180.0 / PI
        val estLon = refLon + (estXm / (EARTH_RADIUS_M * cosRefLat)) * 180.0 / PI

        // Baseline = max pairwise distance between sample positions.
        var baseline = 0.0
        for (i in locals.indices) {
            for (j in i + 1 until locals.size) {
                val dx = locals[i].xM - locals[j].xM
                val dy = locals[i].yM - locals[j].yM
                val d = sqrt(dx * dx + dy * dy)
                if (d > baseline) baseline = d
            }
        }

        // Confidence:
        // - Sample contribution: saturating logistic on count, max 0.5 at >=10
        // - Spread contribution: 0.0 at 0m, 0.4 at >=10m baseline, capped
        // - Penalty for poor GPS: subtract avg accuracy / 50, capped
        val countTerm = (samples.size.toDouble() / 10.0).coerceAtMost(0.5)
        val spreadTerm = (baseline / 25.0).coerceAtMost(0.4)
        val avgAccuracy = samples.map { it.accuracyM }.average()
        val accuracyPenalty = (avgAccuracy / 50.0).coerceAtMost(0.3)
        val confidence = (countTerm + spreadTerm - accuracyPenalty + 0.1).coerceIn(0.0, 1.0)

        return TriangulationResult(
            bssid = bssid,
            estimatedLat = estLat,
            estimatedLon = estLon,
            confidence = confidence,
            sampleCount = samples.size,
            baselineMetres = baseline,
        )
    }

    /**
     * Distance between two WGS84 points using the haversine formula.
     * Useful to caller for displaying "X metres from your current position".
     */
    fun distanceMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = kotlin.math.sin(dLat / 2).pow(2.0) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            kotlin.math.sin(dLon / 2).pow(2.0)
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }
}
