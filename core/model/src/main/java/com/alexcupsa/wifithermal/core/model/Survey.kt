package com.alexcupsa.wifithermal.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Survey(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val floorPlanId: Long? = null,
    val status: SurveyStatus = SurveyStatus.ACTIVE,
    val createdAt: Long = 0, // epoch millis
    val completedAt: Long? = null,
    val totalPoints: Int = 0,
    val totalAps: Int = 0,
    val durationMs: Long = 0,
    val notes: String? = null,
)
