package com.alexcupsa.wifithermal.core.engine.signal

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * Log-distance path loss model for WiFi RSSI-to-distance estimation.
 *
 * The model is:
 *     RSSI(d) = RSSI(d0) - 10 * n * log10(d / d0)
 *
 * where d0 = 1 m, n is the path-loss exponent, and RSSI(d0) is the reference
 * RSSI at 1 m derived from the Friis free-space equation minus a 3 dB margin
 * to account for real-world antenna inefficiency and near-field effects.
 *
 * Typical exponent values:
 *   - Free space:    2.0
 *   - Open office:   2.5-3.0
 *   - Dense office:  3.0-4.0
 *   - Multi-wall:    4.0-6.0
 */
object PathLossModel {

    /**
     * Compute the reference RSSI at 1 m for the given frequency.
     *
     * Uses the Friis free-space path loss at d = 1 m:
     *     FSPL(1m) = 20*log10(f_Hz) + 20*log10(1) - 147.55
     *
     * Since log10(1) = 0, this simplifies to 20*log10(f_Hz) - 147.55.
     * Subtract a 3 dB margin from [txPowerDbm] (default 20 dBm = 100 mW,
     * typical for consumer APs) to account for antenna inefficiency.
     */
    fun referenceRssi(frequencyMhz: Int, txPowerDbm: Double = 20.0): Double {
        val fspl1m = 20 * log10(frequencyMhz * 1e6) - 147.55
        return txPowerDbm - fspl1m - 3.0
    }

    /**
     * Estimate the distance (in metres) from an AP given the measured [rssi].
     *
     * Result is clamped to [0.1, 200.0] m to avoid degenerate values from
     * extremely strong or weak signals.
     */
    fun estimateDistance(
        rssi: Double,
        frequencyMhz: Int,
        pathLossExponent: Double = 3.0,
    ): Double {
        val refRssi = referenceRssi(frequencyMhz)
        return 10.0.pow((refRssi - rssi) / (10 * pathLossExponent))
            .coerceIn(0.1, 200.0)
    }

    /**
     * Predict the RSSI at a given [distance] (metres) from an AP.
     *
     * Distance is floored at 0.1 m to avoid log10(0) singularity.
     */
    fun predictRssi(
        distance: Double,
        frequencyMhz: Int,
        pathLossExponent: Double = 3.0,
    ): Double {
        val refRssi = referenceRssi(frequencyMhz)
        return refRssi - 10 * pathLossExponent * log10(distance.coerceAtLeast(0.1))
    }

    /**
     * Calibrate the path-loss exponent from known (distance, rssi) pairs.
     *
     * Uses ordinary least-squares on the linearised model:
     *     n = sum(RSSI_ref - RSSI_i) / sum(10 * log10(d_i))
     *
     * Returns a value clamped to [1.5, 6.0]. Falls back to 3.0 if the
     * denominator is near zero (all measurements at essentially the same
     * distance).
     *
     * @param measurements List of (distance_m, rssi_dBm) pairs.
     */
    fun calibrateExponent(
        measurements: List<Pair<Double, Double>>,
        frequencyMhz: Int,
    ): Double {
        val refRssi = referenceRssi(frequencyMhz)
        var numerator = 0.0
        var denominator = 0.0

        for ((dist, rssi) in measurements) {
            if (dist > 0.1) {
                numerator += (refRssi - rssi)
                denominator += 10 * log10(dist)
            }
        }

        return if (abs(denominator) > 0.001) {
            (numerator / denominator).coerceIn(1.5, 6.0)
        } else {
            3.0
        }
    }
}
