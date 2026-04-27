package com.alexcupsa.wifithermal.core.engine.position

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Heading (yaw) estimator using a complementary filter that fuses gyroscope
 * and magnetometer readings.
 *
 * The complementary filter combines:
 * - **Gyroscope**: Good short-term accuracy, drifts over time.
 * - **Magnetometer**: No drift, but noisy and susceptible to local magnetic
 *   disturbances (hard/soft iron effects, metal structures).
 *
 * The filter equation per update:
 *
 *     heading = alpha * (heading + gyroYaw * dt) + (1 - alpha) * magHeading
 *
 * where [alpha] in [0, 1] controls the weighting:
 * - alpha = 0.98 (default): strongly favours the gyroscope; slow mag correction
 * - alpha = 0.50: equal weight
 * - alpha = 0.0: pure magnetometer (no gyro integration)
 *
 * The heading is maintained in the range [0, 2 * PI) radians.
 */
class HeadingEstimator(
    /** Weight of the gyroscope in the complementary filter. */
    private val alpha: Double = 0.98,
) {
    /** Current heading estimate in radians [0, 2PI). */
    var headingRad: Double = 0.0
        private set

    private var initialized: Boolean = false
    private var lastTimestampMs: Long = 0

    /**
     * Update the heading from gyroscope and magnetometer readings.
     *
     * @param gyroYawRadPerSec Angular velocity around the vertical (yaw) axis
     *                         in rad/s (positive = counter-clockwise when
     *                         viewed from above, following the right-hand rule).
     * @param magHeadingRad    Absolute heading from the magnetometer in radians
     *                         [0, 2PI). This can be derived from
     *                         [computeMagHeading].
     * @param timestampMs      Sample timestamp in epoch milliseconds.
     * @return Updated heading in radians [0, 2PI).
     */
    fun update(
        gyroYawRadPerSec: Double,
        magHeadingRad: Double,
        timestampMs: Long,
    ): Double {
        if (!initialized) {
            headingRad = normalise(magHeadingRad)
            lastTimestampMs = timestampMs
            initialized = true
            return headingRad
        }

        val dtSec = (timestampMs - lastTimestampMs) / 1000.0
        lastTimestampMs = timestampMs

        if (dtSec <= 0.0 || dtSec > 1.0) {
            // Unreasonable dt -- skip gyro integration, snap to mag
            headingRad = normalise(magHeadingRad)
            return headingRad
        }

        // Gyro-integrated heading
        val gyroHeading = normalise(headingRad + gyroYawRadPerSec * dtSec)

        // Complementary filter using circular interpolation to avoid
        // discontinuities at the 0/2PI boundary.
        headingRad = circularInterpolate(gyroHeading, normalise(magHeadingRad), 1.0 - alpha)

        return headingRad
    }

    /**
     * Alternative update using the Android rotation vector sensor output.
     *
     * The rotation vector sensor already fuses accelerometer, gyroscope, and
     * magnetometer data. We extract the yaw (heading) component.
     *
     * @param rotationVector 4- or 5-element rotation vector from
     *                       TYPE_ROTATION_VECTOR (x, y, z, cos, [accuracy]).
     * @return Updated heading in radians [0, 2PI).
     */
    fun updateFromRotationVector(rotationVector: FloatArray): Double {
        require(rotationVector.size >= 4) {
            "Rotation vector must have at least 4 elements, got ${rotationVector.size}"
        }

        // Convert quaternion to yaw (heading around vertical axis).
        // Quaternion components: (x, y, z, w)
        val x = rotationVector[0].toDouble()
        val y = rotationVector[1].toDouble()
        val z = rotationVector[2].toDouble()
        val w = rotationVector[3].toDouble()

        // Yaw = atan2(2*(w*z + x*y), 1 - 2*(y^2 + z^2))
        val yaw = atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z))

        headingRad = normalise(yaw)
        initialized = true
        return headingRad
    }

    /** Current heading in degrees [0, 360). */
    fun headingDegrees(): Double = headingRad * 180.0 / PI

    /** Reset the estimator to uninitialized state. */
    fun reset() {
        headingRad = 0.0
        initialized = false
        lastTimestampMs = 0
    }

    companion object {
        /**
         * Compute an absolute heading from raw magnetometer readings.
         *
         * @param mx Magnetic field along the X axis (micro-Tesla).
         * @param my Magnetic field along the Y axis (micro-Tesla).
         * @return Heading in radians [0, 2PI), where 0 = magnetic north.
         */
        fun computeMagHeading(mx: Double, my: Double): Double {
            // atan2(-my, mx) gives heading with 0 = north, increasing clockwise
            return normalise(atan2(-my, mx))
        }

        /** Normalise an angle to [0, 2PI). */
        internal fun normalise(angle: Double): Double {
            var a = angle % (2 * PI)
            if (a < 0) a += 2 * PI
            return a
        }

        /**
         * Circular (angular) interpolation that correctly handles the
         * 0/2PI wrap-around boundary.
         *
         * @param a First angle (radians).
         * @param b Second angle (radians).
         * @param t Interpolation factor: 0.0 = a, 1.0 = b.
         */
        internal fun circularInterpolate(a: Double, b: Double, t: Double): Double {
            var delta = b - a
            // Wrap delta to [-PI, PI]
            while (delta > PI) delta -= 2 * PI
            while (delta < -PI) delta += 2 * PI
            return normalise(a + t * delta)
        }
    }
}
