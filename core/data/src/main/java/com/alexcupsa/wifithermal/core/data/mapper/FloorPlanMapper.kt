package com.alexcupsa.wifithermal.core.data.mapper

import com.alexcupsa.wifithermal.core.database.entity.FloorPlanEntity
import com.alexcupsa.wifithermal.core.model.FloorPlan

fun FloorPlanEntity.toDomain(): FloorPlan = FloorPlan(
    id = id,
    name = name,
    imagePath = imagePath,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    scaleMetersPerPixel = scaleMetersPerPixel,
    originX = originX,
    originY = originY,
    rotation = rotation,
    floors = floors,
    createdAt = createdAt,
)

fun FloorPlan.toEntity(): FloorPlanEntity = FloorPlanEntity(
    id = id,
    name = name,
    imagePath = imagePath,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    scaleMetersPerPixel = scaleMetersPerPixel,
    originX = originX,
    originY = originY,
    rotation = rotation,
    floors = floors,
    createdAt = createdAt,
)
