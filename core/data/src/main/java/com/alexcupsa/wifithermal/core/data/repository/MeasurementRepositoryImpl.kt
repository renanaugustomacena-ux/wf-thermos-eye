package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.data.mapper.toDomain
import com.alexcupsa.wifithermal.core.data.mapper.toEntity
import com.alexcupsa.wifithermal.core.database.dao.MeasurementDao
import com.alexcupsa.wifithermal.core.database.dao.SurveyDao
import com.alexcupsa.wifithermal.core.model.MeasurementPoint
import com.alexcupsa.wifithermal.core.model.MeasurementSample
import com.alexcupsa.wifithermal.core.model.WifiBand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepositoryImpl @Inject constructor(
    private val measurementDao: MeasurementDao,
    private val surveyDao: SurveyDao,
) : MeasurementRepository {

    override fun getMeasurements(surveyId: Long): Flow<List<MeasurementPoint>> =
        measurementDao.getMeasurementsWithAps(surveyId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun addMeasurement(surveyId: Long, point: MeasurementPoint) {
        val pointEntity = point.toEntity(surveyId)
        val apEntities = point.apMeasurements.map { it.toEntity(0) }
        measurementDao.insertMeasurementWithAps(pointEntity, apEntities)
        val count = measurementDao.getPointCount(surveyId)
        surveyDao.updatePointCount(surveyId, count)
    }

    override suspend fun deleteMeasurement(pointId: Long) {
        measurementDao.deleteMeasurementPoint(pointId)
    }

    override suspend fun getMeasurementSamples(surveyId: Long, bssid: String): List<MeasurementSample> =
        measurementDao.getMeasurementSamples(surveyId, bssid).map {
            MeasurementSample(x = it.x, y = it.y, rssi = it.rssi)
        }

    override suspend fun getBandMeasurementSamples(surveyId: Long, band: WifiBand): List<MeasurementSample> =
        measurementDao.getBandMeasurementSamples(surveyId, band.name).map {
            MeasurementSample(x = it.x, y = it.y, rssi = it.rssi)
        }

    override suspend fun getApSummary(surveyId: Long): List<ApSummary> =
        measurementDao.getApSummary(surveyId).map {
            ApSummary(
                bssid = it.bssid,
                ssid = it.ssid,
                minRssi = it.minRssi,
                maxRssi = it.maxRssi,
                avgRssi = it.avgRssi,
                measurementCount = it.measurementCount,
            )
        }

    override suspend fun getPointCount(surveyId: Long): Int =
        measurementDao.getPointCount(surveyId)
}
