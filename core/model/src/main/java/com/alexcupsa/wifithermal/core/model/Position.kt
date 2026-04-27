package com.alexcupsa.wifithermal.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Position(
    val x: Double,
    val y: Double,
    val floor: Int = 0,
    val confidence: Double = 1.0,
)
