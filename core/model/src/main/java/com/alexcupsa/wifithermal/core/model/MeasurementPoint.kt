package com.alexcupsa.wifithermal.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementPoint(
    val id: Long = 0,
    val position: Position,
    val timestamp: Long, // epoch millis
    val apMeasurements: List<ApMeasurement> = emptyList(),
)
