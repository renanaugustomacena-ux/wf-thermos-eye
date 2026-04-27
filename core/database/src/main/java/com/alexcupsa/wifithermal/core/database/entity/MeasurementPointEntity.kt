package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_points",
    foreignKeys = [ForeignKey(
        entity = SurveyEntity::class,
        parentColumns = ["id"],
        childColumns = ["surveyId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("surveyId")],
)
data class MeasurementPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyId: Long,
    val x: Double,
    val y: Double,
    val floor: Int,
    val positionConfidence: Double,
    val timestamp: Long,
)
