package com.alexcupsa.wifithermal.core.engine.heatmap

import com.alexcupsa.wifithermal.core.model.ColorScheme

/**
 * Pre-computed 256-entry colour look-up tables for heatmap rendering.
 *
 * Each LUT maps a normalised index [0..255] to an ARGB colour. The index
 * represents signal strength from strongest (0 = best RSSI) to weakest
 * (255 = worst RSSI).
 *
 * Colour schemes:
 * - **THERMAL**: Red -> Orange -> Yellow -> Green -> Cyan -> Blue (strong to weak)
 * - **VIRIDIS**: Perceptually-uniform purple -> teal -> yellow
 * - **MAGMA**:   Black -> purple -> orange -> yellow
 * - **GRAYSCALE**: White (strong) -> black (weak)
 *
 * The LUTs are built once on first access (lazy initialisation) and reused
 * for all subsequent calls.
 */
object HeatmapColors {

    private val thermalLut: IntArray by lazy { buildThermalLut() }
    private val viridisLut: IntArray by lazy { buildViridisLut() }
    private val magmaLut: IntArray by lazy { buildMagmaLut() }
    private val grayscaleLut: IntArray by lazy { buildGrayscaleLut() }

    /**
     * Map an RSSI value to an ARGB colour integer.
     *
     * @param rssi     Measured or interpolated RSSI in dBm.
     * @param scheme   Colour scheme to use.
     * @param minRssi  RSSI value mapped to index 255 (weakest colour).
     * @param maxRssi  RSSI value mapped to index 0 (strongest colour).
     * @param alpha    Alpha channel value [0..255]. Overrides the LUT alpha.
     * @return Packed ARGB colour integer.
     */
    fun rssiToColor(
        rssi: Double,
        scheme: ColorScheme,
        minRssi: Double = -90.0,
        maxRssi: Double = -30.0,
        alpha: Int = 255,
    ): Int {
        val clampedAlpha = alpha.coerceIn(0, 255)

        // Normalise: 0.0 = maxRssi (strong), 1.0 = minRssi (weak)
        val t = if (maxRssi > minRssi) {
            ((maxRssi - rssi) / (maxRssi - minRssi)).coerceIn(0.0, 1.0)
        } else {
            0.5
        }
        val index = (t * 255).toInt().coerceIn(0, 255)

        val lut = when (scheme) {
            ColorScheme.THERMAL -> thermalLut
            ColorScheme.VIRIDIS -> viridisLut
            ColorScheme.MAGMA -> magmaLut
            ColorScheme.GRAYSCALE -> grayscaleLut
        }

        val rgb = lut[index] and 0x00FFFFFF
        return (clampedAlpha shl 24) or rgb
    }

    // -------------------------------------------------------------------------
    // LUT builders
    // -------------------------------------------------------------------------

    /**
     * THERMAL: Red(strong) -> Orange -> Yellow -> Green -> Cyan -> Blue(weak).
     * 6 control points, linearly interpolated.
     */
    private fun buildThermalLut(): IntArray {
        val stops = listOf(
            0.0 to Triple(255, 0, 0),       // Red
            0.2 to Triple(255, 140, 0),      // Orange
            0.4 to Triple(255, 255, 0),      // Yellow
            0.6 to Triple(0, 200, 0),        // Green
            0.8 to Triple(0, 220, 255),      // Cyan
            1.0 to Triple(0, 0, 200),        // Blue
        )
        return buildGradientLut(stops)
    }

    /**
     * VIRIDIS: Perceptually uniform, purple -> teal -> yellow.
     * Approximation using 5 key stops sampled from the matplotlib viridis map.
     */
    private fun buildViridisLut(): IntArray {
        val stops = listOf(
            0.0 to Triple(253, 231, 37),     // Yellow (strong)
            0.25 to Triple(94, 201, 98),     // Green
            0.5 to Triple(33, 145, 140),     // Teal
            0.75 to Triple(59, 82, 139),     // Blue-purple
            1.0 to Triple(68, 1, 84),        // Dark purple (weak)
        )
        return buildGradientLut(stops)
    }

    /**
     * MAGMA: Black -> dark purple -> orange -> yellow.
     * Approximation using 5 key stops sampled from the matplotlib magma map.
     */
    private fun buildMagmaLut(): IntArray {
        val stops = listOf(
            0.0 to Triple(252, 253, 191),    // Light yellow (strong)
            0.25 to Triple(254, 159, 109),   // Peach-orange
            0.5 to Triple(183, 55, 121),     // Magenta
            0.75 to Triple(81, 18, 124),     // Deep purple
            1.0 to Triple(0, 0, 4),          // Near-black (weak)
        )
        return buildGradientLut(stops)
    }

    /**
     * GRAYSCALE: White (strong) -> Black (weak).
     */
    private fun buildGrayscaleLut(): IntArray {
        return IntArray(256) { i ->
            val v = 255 - i // 255 at index 0, 0 at index 255
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
    }

    /**
     * Build a 256-entry LUT by linearly interpolating between colour stops.
     *
     * @param stops Sorted list of (position, RGB) where position is in [0, 1].
     */
    private fun buildGradientLut(stops: List<Pair<Double, Triple<Int, Int, Int>>>): IntArray {
        val lut = IntArray(256)

        for (i in 0..255) {
            val t = i / 255.0

            // Find the two stops that bracket t
            var lower = stops.first()
            var upper = stops.last()
            for (j in 0 until stops.size - 1) {
                if (t >= stops[j].first && t <= stops[j + 1].first) {
                    lower = stops[j]
                    upper = stops[j + 1]
                    break
                }
            }

            val range = upper.first - lower.first
            val frac = if (range > 0.0) (t - lower.first) / range else 0.0

            val r = lerp(lower.second.first, upper.second.first, frac)
            val g = lerp(lower.second.second, upper.second.second, frac)
            val b = lerp(lower.second.third, upper.second.third, frac)

            lut[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        return lut
    }

    private fun lerp(a: Int, b: Int, t: Double): Int =
        (a + (b - a) * t).toInt().coerceIn(0, 255)
}
