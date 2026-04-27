package com.alexcupsa.wifithermal.core.engine.heatmap

import com.alexcupsa.wifithermal.core.model.MeasurementSample
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Inverse Distance Weighting (IDW) interpolation for heatmap generation.
 *
 * Shepard's method: for a query point **p**, the interpolated value is
 *
 *     z(p) = sum( w_i * z_i ) / sum( w_i )
 *
 * where w_i = 1 / d(p, p_i)^power and d is the Euclidean distance.
 *
 * Grid cells that fall exactly on a measurement point receive that point's
 * exact value (no division by zero). Cells outside [searchRadius] of all
 * measurement points are set to [Double.NaN] to indicate no data.
 */
object IdwInterpolator {

    /**
     * Interpolate a grid of RSSI values from sparse measurements.
     *
     * @param samples     Known measurement points with (x, y, rssi).
     * @param width       Grid width in pixels/cells.
     * @param height      Grid height in pixels/cells.
     * @param power       Distance weighting exponent (default 2.0). Higher
     *                    values give more local influence.
     * @param searchRadius Maximum distance (in grid units) to consider
     *                     measurement points. Points beyond this radius are
     *                     ignored. Use [Double.MAX_VALUE] for unlimited range.
     * @return [DoubleArray] of size width*height in row-major order. Cells
     *         with no nearby measurements are [Double.NaN].
     */
    fun interpolate(
        samples: List<MeasurementSample>,
        width: Int,
        height: Int,
        power: Double = 2.0,
        searchRadius: Double = Double.MAX_VALUE,
    ): DoubleArray {
        require(width > 0 && height > 0) { "Grid dimensions must be positive" }
        require(samples.isNotEmpty()) { "At least one measurement sample is required" }

        val grid = DoubleArray(width * height) { Double.NaN }
        val searchRadiusSq = searchRadius * searchRadius

        for (row in 0 until height) {
            for (col in 0 until width) {
                val gx = col.toDouble()
                val gy = row.toDouble()

                var weightedSum = 0.0
                var weightSum = 0.0
                var exactMatch = false

                for (sample in samples) {
                    val dx = gx - sample.x
                    val dy = gy - sample.y
                    val distSq = dx * dx + dy * dy

                    // Skip samples outside the search radius
                    if (distSq > searchRadiusSq) continue

                    // Exact match: grid point coincides with a measurement
                    if (distSq < 1e-10) {
                        grid[row * width + col] = sample.rssi
                        exactMatch = true
                        break
                    }

                    val dist = sqrt(distSq)
                    val weight = 1.0 / dist.pow(power)
                    weightedSum += weight * sample.rssi
                    weightSum += weight
                }

                if (!exactMatch && weightSum > 0.0) {
                    grid[row * width + col] = weightedSum / weightSum
                }
            }
        }

        return grid
    }
}
