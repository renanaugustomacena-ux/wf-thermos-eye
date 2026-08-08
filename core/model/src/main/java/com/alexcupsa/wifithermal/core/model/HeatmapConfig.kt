package com.alexcupsa.wifithermal.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HeatmapConfig(
    val algorithm: InterpolationAlgorithm = InterpolationAlgorithm.IDW,
    val idwPower: Double = 2.0,
    val rbfEpsilon: Double = 1.0,
    val smoothingRadius: Int = 5,
    val colorScheme: ColorScheme = ColorScheme.THERMAL,
    val minRssi: Double = -90.0,
    val maxRssi: Double = -30.0,
    val alpha: Int = 180,
    val resolution: Float = 1.0f,
)

data class MeasurementSample(
    val x: Double,
    val y: Double,
    val rssi: Double,
)

data class HeatmapGrid(
    val data: DoubleArray,
    val width: Int,
    val height: Int,
    val minValue: Double,
    val maxValue: Double,
    val generationTimeMs: Long = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeatmapGrid) return false
        return width == other.width && height == other.height && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = 31 * width + height
}
