package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.model.CoordinateTransform
import com.alexcupsa.wifithermal.core.model.FloorPlan
import kotlinx.coroutines.flow.Flow

interface FloorPlanRepository {
    fun getAllFloorPlans(): Flow<List<FloorPlan>>
    suspend fun getFloorPlan(id: Long): FloorPlan?
    suspend fun saveFloorPlan(name: String, imagePath: String, width: Int, height: Int): Long
    suspend fun deleteFloorPlan(id: Long)
    suspend fun updateCalibration(id: Long, transform: CoordinateTransform)
}
