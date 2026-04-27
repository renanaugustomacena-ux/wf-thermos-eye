package com.alexcupsa.wifithermal.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "floor_plans")
data class FloorPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imagePath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val scaleMetersPerPixel: Double,
    val originX: Double,
    val originY: Double,
    val rotation: Double,
    val floors: Int,
    val createdAt: Long,
)
