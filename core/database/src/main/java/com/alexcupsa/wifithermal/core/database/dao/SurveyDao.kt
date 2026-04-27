package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alexcupsa.wifithermal.core.database.entity.SurveyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {

    @Query("SELECT * FROM surveys ORDER BY createdAt DESC")
    fun getAllSurveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE id = :id")
    suspend fun getSurveyById(id: Long): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE status = :status ORDER BY createdAt DESC")
    fun getSurveysByStatus(status: String): Flow<List<SurveyEntity>>

    @Insert
    suspend fun insert(survey: SurveyEntity): Long

    @Update
    suspend fun update(survey: SurveyEntity)

    @Delete
    suspend fun delete(survey: SurveyEntity)

    @Query("UPDATE surveys SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun completeSurvey(id: Long, status: String, completedAt: Long)

    @Query("UPDATE surveys SET totalPoints = :count WHERE id = :id")
    suspend fun updatePointCount(id: Long, count: Int)

    @Query("UPDATE surveys SET totalAps = :count WHERE id = :id")
    suspend fun updateApCount(id: Long, count: Int)
}
