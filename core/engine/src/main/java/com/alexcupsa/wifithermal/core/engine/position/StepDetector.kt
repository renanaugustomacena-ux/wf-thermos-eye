package com.alexcupsa.wifithermal.core.engine.position

import kotlin.math.sqrt

/**
 * Step detection from tri-axial accelerometer data using peak-valley analysis.
 *
 * Algorithm:
 * 1. Compute the magnitude of the accelerometer vector: |a| = sqrt(ax^2 + ay^2 + az^2).
 * 2. Detect a step when a peak exceeding [peakThreshold] is followed by a
 *    valley below [valleyThreshold].
 * 3. Enforce a minimum time interval ([minStepIntervalMs]) between consecutive
 *    steps to suppress double-counting from gait harmonics.
 *
 * Typical values for a phone in a pocket or hand:
 * - peakThreshold:  11.0 m/s^2  (gravity ~ 9.81, peak during heel strike ~ 12-15)
 * - valleyThreshold: 8.0 m/s^2  (below gravity during swing phase)
 * - minStepIntervalMs: 300 ms   (limits to ~3.3 steps/sec = fast jog)
 */
class StepDetector(
    private val peakThreshold: Double = 11.0,
    private val valleyThreshold: Double = 8.0,
    private val minStepIntervalMs: Long = 300,
) {
    /** Callback invoked on each detected step with the timestamp in milliseconds. */
    var onStep: ((timestampMs: Long) -> Unit)? = null

    private var stepCount: Long = 0
    private var lastStepTimestampMs: Long = 0
    private var peakDetected: Boolean = false
    private var lastPeakMagnitude: Double = 0.0

    /**
     * Feed a new accelerometer sample.
     *
     * @param ax Acceleration along the X axis (m/s^2).
     * @param ay Acceleration along the Y axis (m/s^2).
     * @param az Acceleration along the Z axis (m/s^2).
     * @param timestampMs Sample timestamp in epoch milliseconds.
     * @return `true` if a step was detected on this sample.
     */
    fun onAccelerometerSample(
        ax: Double,
        ay: Double,
        az: Double,
        timestampMs: Long,
    ): Boolean {
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        return processMagnitude(magnitude, timestampMs)
    }

    /**
     * Feed a pre-computed acceleration magnitude.
     *
     * @param magnitude Acceleration magnitude in m/s^2.
     * @param timestampMs Sample timestamp in epoch milliseconds.
     * @return `true` if a step was detected on this sample.
     */
    fun processMagnitude(magnitude: Double, timestampMs: Long): Boolean {
        // Phase 1: Look for a peak above the threshold
        if (!peakDetected) {
            if (magnitude > peakThreshold) {
                peakDetected = true
                lastPeakMagnitude = magnitude
            }
            return false
        }

        // Track the highest peak value (in case the peak is multi-sample)
        if (magnitude > lastPeakMagnitude) {
            lastPeakMagnitude = magnitude
        }

        // Phase 2: Look for a valley below the threshold after a peak
        if (magnitude < valleyThreshold) {
            peakDetected = false

            // Enforce minimum step interval
            if (timestampMs - lastStepTimestampMs >= minStepIntervalMs) {
                stepCount++
                lastStepTimestampMs = timestampMs
                onStep?.invoke(timestampMs)
                return true
            }
        }

        return false
    }

    /** Total number of steps detected since creation or last [reset]. */
    fun stepCount(): Long = stepCount

    /** Timestamp (ms) of the most recently detected step, or 0 if none. */
    fun lastStepTimestamp(): Long = lastStepTimestampMs

    /** Reset the detector state and step counter to zero. */
    fun reset() {
        stepCount = 0
        lastStepTimestampMs = 0
        peakDetected = false
        lastPeakMagnitude = 0.0
    }
}
