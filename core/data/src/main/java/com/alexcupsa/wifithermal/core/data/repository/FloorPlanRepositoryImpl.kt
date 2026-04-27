package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.data.mapper.toDomain
import com.alexcupsa.wifithermal.core.database.dao.FloorPlanDao
import com.alexcupsa.wifithermal.core.database.entity.FloorPlanEntity
import com.alexcupsa.wifithermal.core.model.CoordinateTransform
import com.alexcupsa.wifithermal.core.model.FloorPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FloorPlanRepositoryImpl @Inject constructor(
    private val floorPlanDao: FloorPlanDao,
) : FloorPlanRepository {

    override fun getAllFloorPlans(): Flow<List<FloorPlan>> =
        floorPlanDao.getAllFloorPlans().map { list -> list.map { it.toDomain() } }

    override suspend fun getFloorPlan(id: Long): FloorPlan? =
        floorPlanDao.getFloorPlanById(id)?.toDomain()

    override suspend fun saveFloorPlan(name: String, imagePath: String, width: Int, height: Int): Long =
        floorPlanDao.insert(
            FloorPlanEntity(
                name = name,
                imagePath = imagePath,
                imageWidth = width,
                imageHeight = height,
                scaleMetersPerPixel = 0.05,
                originX = 0.0,
                originY = 0.0,
                rotation = 0.0,
                floors = 1,
                createdAt = System.currentTimeMillis(),
            )
        )

    override suspend fun deleteFloorPlan(id: Long) {
        floorPlanDao.getFloorPlanById(id)?.let { floorPlanDao.delete(it) }
    }

    override suspend fun updateCalibration(id: Long, transform: CoordinateTransform) {
        floorPlanDao.updateCalibration(
            id = id,
            scale = transform.scaleMetersPerPixel,
            originX = transform.originX,
            originY = transform.originY,
            rotation = transform.rotation,
        )
    }
}
