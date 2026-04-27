package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.model.Survey
import com.alexcupsa.wifithermal.core.model.SurveyStatus
import kotlinx.coroutines.flow.Flow

interface SurveyRepository {
    fun getAllSurveys(): Flow<List<Survey>>
    fun getSurveysByStatus(status: SurveyStatus): Flow<List<Survey>>
    suspend fun getSurveyById(id: Long): Survey?
    suspend fun createSurvey(name: String, description: String?, floorPlanId: Long?): Long
    suspend fun completeSurvey(id: Long)
    suspend fun deleteSurvey(id: Long)
}
