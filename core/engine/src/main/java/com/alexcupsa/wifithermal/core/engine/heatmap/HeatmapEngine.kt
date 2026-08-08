package com.alexcupsa.wifithermal.core.engine.heatmap

import com.alexcupsa.wifithermal.core.model.HeatmapConfig
import com.alexcupsa.wifithermal.core.model.HeatmapGrid
import com.alexcupsa.wifithermal.core.model.InterpolationAlgorithm
import com.alexcupsa.wifithermal.core.model.MeasurementSample

/**
 * Orchestrator for heatmap generation.
 *
 * Pipeline:
 * 1. Validate input (minimum 3 samples).
 * 2. Interpolate sparse measurements onto a regular grid via IDW.
 * 3. Smooth the grid with a Gaussian blur.
 * 4. Compute min/max values and timing metadata.
 *
 * This class is stateless and thread-safe.
 */
object HeatmapEngine {

    /** Minimum number of measurement samples required for interpolation. */
    private const val MIN_SAMPLES = 3

    /**
     * Generate a heatmap grid from sparse measurement samples.
     *
     * @param samples List of measurement samples with (x, y, rssi).
     * @param width   Output grid width in cells.
     * @param height  Output grid height in cells.
     * @param config  Heatmap configuration (algorithm, smoothing, colour, etc.).
     * @return [HeatmapGrid] containing the interpolated and smoothed data.
     * @throws IllegalArgumentException if there are fewer than [MIN_SAMPLES]
     *         samples or if dimensions are non-positive.
     */
    fun generate(
        samples: List<MeasurementSample>,
        width: Int,
        height: Int,
        config: HeatmapConfig = HeatmapConfig(),
    ): HeatmapGrid {
        require(samples.size >= MIN_SAMPLES) {
            "At least $MIN_SAMPLES measurement samples are required, got ${samples.size}"
        }
        require(width > 0 && height > 0) {
            "Grid dimensions must be positive ($width x $height)"
        }

        val startNanos = System.nanoTime()

        // Step 1: Interpolate
        val interpolated = when (config.algorithm) {
            InterpolationAlgorithm.IDW -> IdwInterpolator.interpolate(
                samples = samples,
                width = width,
                height = height,
                power = config.idwPower,
            )
            // Other algorithms could be plugged in here; for now IDW is used
            // as fallback for all selections.
            else -> IdwInterpolator.interpolate(
                samples = samples,
                width = width,
                height = height,
                power = config.idwPower,
            )
        }

        // Step 2: Smooth
        val smoothed = if (config.smoothingRadius > 0) {
            GaussianBlur.apply(interpolated, width, height, config.smoothingRadius)
        } else {
            interpolated
        }

        // Step 3: Compute statistics (ignoring NaN cells)
        var minValue = Double.MAX_VALUE
        var maxValue = -Double.MAX_VALUE
        for (v in smoothed) {
            if (!v.isNaN()) {
                if (v < minValue) minValue = v
                if (v > maxValue) maxValue = v
            }
        }
        // If all cells are NaN, fall back to config bounds
        if (minValue == Double.MAX_VALUE) {
            minValue = config.minRssi
            maxValue = config.maxRssi
        }

        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L

        return HeatmapGrid(
            data = smoothed,
            width = width,
            height = height,
            minValue = minValue,
            maxValue = maxValue,
            generationTimeMs = elapsedMs,
        )
    }
}
