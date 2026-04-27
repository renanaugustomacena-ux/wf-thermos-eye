package com.alexcupsa.wifithermal.core.engine.heatmap

import kotlin.math.exp

/**
 * Separable 2D Gaussian blur for heatmap smoothing.
 *
 * Performs two 1D passes (horizontal then vertical) for O(n * r) per pixel
 * instead of O(n * r^2). NaN cells are excluded from the weighted average
 * so that "no data" regions do not bleed into measured areas.
 */
object GaussianBlur {

    /**
     * Apply a Gaussian blur to a grid of RSSI values.
     *
     * @param grid   Input grid in row-major order (may contain [Double.NaN]).
     * @param width  Grid width.
     * @param height Grid height.
     * @param radius Blur kernel radius in cells. The full kernel width is
     *               2 * radius + 1. A radius of 0 returns a copy of [grid].
     * @return A new [DoubleArray] of the same size with the blur applied.
     *         NaN cells that have no non-NaN neighbours within the kernel
     *         remain NaN.
     */
    fun apply(
        grid: DoubleArray,
        width: Int,
        height: Int,
        radius: Int,
    ): DoubleArray {
        require(grid.size == width * height) {
            "Grid size (${grid.size}) does not match dimensions ($width x $height)"
        }
        if (radius <= 0) return grid.copyOf()

        val kernel = buildKernel(radius)

        // Horizontal pass
        val horizontal = DoubleArray(grid.size) { Double.NaN }
        for (row in 0 until height) {
            for (col in 0 until width) {
                horizontal[row * width + col] = convolve1d(
                    grid, width, height, row, col, kernel, radius, horizontal = true
                )
            }
        }

        // Vertical pass
        val result = DoubleArray(grid.size) { Double.NaN }
        for (row in 0 until height) {
            for (col in 0 until width) {
                result[row * width + col] = convolve1d(
                    horizontal, width, height, row, col, kernel, radius, horizontal = false
                )
            }
        }

        return result
    }

    /**
     * Build a 1D Gaussian kernel of size 2 * [radius] + 1.
     *
     * The sigma is chosen so that the kernel spans approximately 3 sigma
     * (sigma = radius / 3), which captures > 99.7 % of the distribution.
     * The kernel is NOT normalised here because we normalise dynamically
     * during convolution to handle NaN exclusion.
     */
    private fun buildKernel(radius: Int): DoubleArray {
        val sigma = radius / 3.0
        val twoSigmaSq = 2.0 * sigma * sigma
        val size = 2 * radius + 1
        return DoubleArray(size) { i ->
            val x = (i - radius).toDouble()
            exp(-(x * x) / twoSigmaSq)
        }
    }

    /**
     * Convolve a single cell with the 1D kernel along one axis.
     *
     * NaN values in the source grid are skipped; the kernel weight is
     * redistributed among valid neighbours. If no valid neighbours exist
     * the result is NaN.
     */
    private fun convolve1d(
        grid: DoubleArray,
        width: Int,
        height: Int,
        row: Int,
        col: Int,
        kernel: DoubleArray,
        radius: Int,
        horizontal: Boolean,
    ): Double {
        var weightedSum = 0.0
        var weightSum = 0.0

        for (k in -radius..radius) {
            val r = if (horizontal) row else row + k
            val c = if (horizontal) col + k else col

            if (r < 0 || r >= height || c < 0 || c >= width) continue

            val value = grid[r * width + c]
            if (value.isNaN()) continue

            val w = kernel[k + radius]
            weightedSum += w * value
            weightSum += w
        }

        return if (weightSum > 0.0) weightedSum / weightSum else Double.NaN
    }
}
