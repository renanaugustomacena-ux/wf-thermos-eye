package com.alexcupsa.wifithermal.core.engine.signal

/**
 * Single-state Kalman filter tuned for RSSI smoothing.
 *
 * The state is a scalar (estimated RSSI in dBm). The process model is a
 * random-walk: the next RSSI is the current RSSI plus zero-mean Gaussian noise
 * whose variance is [processNoise]. The measurement model is identity with
 * additive noise of variance [measurementNoise].
 *
 * Typical WiFi RSSI jitter is 2-6 dB, so [measurementNoise] = 4.0 is a
 * reasonable default. [processNoise] = 0.008 allows slow drift while
 * rejecting fast spikes.
 */
class RssiKalmanFilter(
    private val processNoise: Double = 0.008,
    private val measurementNoise: Double = 4.0,
) {
    private var estimate: Double = 0.0
    private var errorCovariance: Double = 1.0
    private var initialized: Boolean = false

    /**
     * Incorporate a new RSSI measurement and return the filtered estimate.
     *
     * On the very first call the filter is seeded with the measurement itself
     * (prior = measurement, initial error covariance = measurementNoise).
     */
    fun update(measurement: Double): Double {
        if (!initialized) {
            estimate = measurement
            errorCovariance = measurementNoise
            initialized = true
            return estimate
        }

        // Predict step (random-walk model: predicted state = current estimate)
        val predictedError = errorCovariance + processNoise

        // Update step
        val kalmanGain = predictedError / (predictedError + measurementNoise)
        estimate += kalmanGain * (measurement - estimate)
        errorCovariance = (1 - kalmanGain) * predictedError

        return estimate
    }

    /** Return the latest filtered estimate without advancing the filter. */
    fun currentEstimate(): Double = estimate

    /** Reset the filter to its uninitialized state. */
    fun reset() {
        initialized = false
        estimate = 0.0
        errorCovariance = 1.0
    }
}
