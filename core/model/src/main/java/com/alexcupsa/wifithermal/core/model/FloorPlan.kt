package com.alexcupsa.wifithermal.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FloorPlan(
    val id: Long = 0,
    val name: String,
    val imagePath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val scaleMetersPerPixel: Double = 0.05,
    val originX: Double = 0.0,
    val originY: Double = 0.0,
    val rotation: Double = 0.0,
    val floors: Int = 1,
    val createdAt: Long = 0,
)
