package com.alexcupsa.wifithermal.core.data.mapper

import com.alexcupsa.wifithermal.core.database.entity.SurveyEntity
import com.alexcupsa.wifithermal.core.model.Survey
import com.alexcupsa.wifithermal.core.model.SurveyStatus

fun SurveyEntity.toDomain(): Survey = Survey(
    id = id,
    name = name,
    description = description,
    floorPlanId = floorPlanId,
    status = try { SurveyStatus.valueOf(status) } catch (_: Exception) { SurveyStatus.ACTIVE },
    createdAt = createdAt,
    completedAt = completedAt,
    totalPoints = totalPoints,
    totalAps = totalAps,
    durationMs = durationMs,
    notes = notes,
)

fun Survey.toEntity(): SurveyEntity = SurveyEntity(
    id = id,
    name = name,
    description = description,
    floorPlanId = floorPlanId,
    createdAt = createdAt,
    completedAt = completedAt,
    status = status.name,
    totalPoints = totalPoints,
    totalAps = totalAps,
    durationMs = durationMs,
    notes = notes,
)
