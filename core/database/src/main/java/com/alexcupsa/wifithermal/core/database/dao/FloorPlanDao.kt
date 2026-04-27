package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alexcupsa.wifithermal.core.database.entity.FloorPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FloorPlanDao {

    @Query("SELECT * FROM floor_plans ORDER BY createdAt DESC")
    fun getAllFloorPlans(): Flow<List<FloorPlanEntity>>

    @Query("SELECT * FROM floor_plans WHERE id = :id")
    suspend fun getFloorPlanById(id: Long): FloorPlanEntity?

    @Insert
    suspend fun insert(floorPlan: FloorPlanEntity): Long

    @Update
    suspend fun update(floorPlan: FloorPlanEntity)

    @Delete
    suspend fun delete(floorPlan: FloorPlanEntity)

    @Query("""
        UPDATE floor_plans
        SET scaleMetersPerPixel = :scale, originX = :originX, originY = :originY, rotation = :rotation
        WHERE id = :id
    """)
    suspend fun updateCalibration(id: Long, scale: Double, originX: Double, originY: Double, rotation: Double)
}
