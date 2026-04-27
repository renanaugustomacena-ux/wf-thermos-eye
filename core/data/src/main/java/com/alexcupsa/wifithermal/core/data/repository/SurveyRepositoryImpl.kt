package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.data.mapper.toDomain
import com.alexcupsa.wifithermal.core.database.dao.SurveyDao
import com.alexcupsa.wifithermal.core.database.entity.SurveyEntity
import com.alexcupsa.wifithermal.core.model.Survey
import com.alexcupsa.wifithermal.core.model.SurveyStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurveyRepositoryImpl @Inject constructor(
    private val surveyDao: SurveyDao,
) : SurveyRepository {

    override fun getAllSurveys(): Flow<List<Survey>> =
        surveyDao.getAllSurveys().map { list -> list.map { it.toDomain() } }

    override fun getSurveysByStatus(status: SurveyStatus): Flow<List<Survey>> =
        surveyDao.getSurveysByStatus(status.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getSurveyById(id: Long): Survey? =
        surveyDao.getSurveyById(id)?.toDomain()

    override suspend fun createSurvey(name: String, description: String?, floorPlanId: Long?): Long =
        surveyDao.insert(
            SurveyEntity(
                name = name,
                description = description,
                floorPlanId = floorPlanId,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                status = SurveyStatus.ACTIVE.name,
                totalPoints = 0,
                totalAps = 0,
                durationMs = 0,
                notes = null,
            )
        )

    override suspend fun completeSurvey(id: Long) {
        surveyDao.completeSurvey(id, SurveyStatus.COMPLETED.name, System.currentTimeMillis())
    }

    override suspend fun deleteSurvey(id: Long) {
        surveyDao.getSurveyById(id)?.let { surveyDao.delete(it) }
    }
}
