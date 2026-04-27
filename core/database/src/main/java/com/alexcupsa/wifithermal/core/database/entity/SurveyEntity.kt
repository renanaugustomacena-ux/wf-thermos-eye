package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val floorPlanId: Long?,
    val createdAt: Long,
    val completedAt: Long?,
    val status: String,
    val totalPoints: Int,
    val totalAps: Int,
    val durationMs: Long,
    val notes: String?,
)
