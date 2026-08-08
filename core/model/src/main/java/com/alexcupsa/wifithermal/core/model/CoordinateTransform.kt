package com.alexcupsa.wifithermal.core.model

import kotlin.math.cos
import kotlin.math.sin
import kotlinx.serialization.Serializable

@Serializable
data class CoordinateTransform(
    val scaleMetersPerPixel: Double,
    val originX: Double,
    val originY: Double,
    val rotation: Double = 0.0,
) {
    fun metersToFloorPlan(mx: Double, my: Double): Pair<Double, Double> {
        val cosR = cos(rotation)
        val sinR = sin(rotation)
        val rx = mx * cosR - my * sinR
        val ry = mx * sinR + my * cosR
        return Pair(originX + rx / scaleMetersPerPixel, originY + ry / scaleMetersPerPixel)
    }

    fun floorPlanToMeters(px: Double, py: Double): Pair<Double, Double> {
        val dx = (px - originX) * scaleMetersPerPixel
        val dy = (py - originY) * scaleMetersPerPixel
        val cosR = cos(-rotation)
        val sinR = sin(-rotation)
        return Pair(dx * cosR - dy * sinR, dx * sinR + dy * cosR)
    }
}
