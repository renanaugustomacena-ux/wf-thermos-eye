package com.alexcupsa.wifithermal.core.engine.position

import kotlin.math.pow

/**
 * Step length estimation using the Weinberg model.
 *
 * The Weinberg model estimates step length from the peak-to-trough amplitude
 * of the vertical acceleration during one gait cycle:
 *
 *     L = k * (a_max - a_min)^0.25
 *
 * where:
 * - L is the step length in metres
 * - k is a calibration constant (default 0.37, typical for walking)
 * - a_max is the maximum acceleration magnitude during the step (m/s^2)
 * - a_min is the minimum acceleration magnitude during the step (m/s^2)
 *
 * Reference: Weinberg, H. (2002). "Using the ADXL202 in Pedometer and
 * Personal Navigation Applications", Analog Devices AN-602.
 */
class StepLengthEstimator(
    /** Weinberg calibration constant. Typical range: 0.30 - 0.45. */
    var k: Double = 0.37,
) {
    /**
     * Estimate step length from peak and trough acceleration magnitudes.
     *
     * @param aMax Maximum acceleration magnitude during the step (m/s^2).
     * @param aMin Minimum acceleration magnitude during the step (m/s^2).
     * @return Estimated step length in metres, clamped to [0.2, 1.5] m
     *         to reject physiologically implausible values.
     */
    fun estimate(aMax: Double, aMin: Double): Double {
        val amplitude = (aMax - aMin).coerceAtLeast(0.0)
        val length = k * amplitude.pow(0.25)
        return length.coerceIn(0.2, 1.5)
    }

    /**
     * Estimate a default step length from the user's height.
     *
     * Empirical approximation: step length is approximately 41.5 % of body
     * height for walking and 45 % for fast walking (Grieve & Gear, 1966).
     *
     * @param heightMetres User height in metres (e.g. 1.75).
     * @return Estimated step length in metres.
     */
    fun fromHeight(heightMetres: Double): Double {
        require(heightMetres > 0.0) { "Height must be positive" }
        return (heightMetres * 0.415).coerceIn(0.2, 1.5)
    }

    /**
     * Calibrate [k] from known step lengths and acceleration amplitudes.
     *
     * @param samples List of (aMax, aMin, knownLengthMetres).
     */
    fun calibrate(samples: List<Triple<Double, Double, Double>>) {
        if (samples.isEmpty()) return

        var sumK = 0.0
        var count = 0
        for ((aMax, aMin, length) in samples) {
            val amplitude = (aMax - aMin).coerceAtLeast(0.001)
            val candidateK = length / amplitude.pow(0.25)
            if (candidateK in 0.1..1.0) {
                sumK += candidateK
                count++
            }
        }

        if (count > 0) {
            k = (sumK / count).coerceIn(0.2, 0.6)
        }
    }
}
