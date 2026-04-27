package com.alexcupsa.wifithermal.core.data.repository

import com.alexcupsa.wifithermal.core.model.MeasurementPoint
import com.alexcupsa.wifithermal.core.model.MeasurementSample
import com.alexcupsa.wifithermal.core.model.WifiBand
import kotlinx.coroutines.flow.Flow

data class ApSummary(
    val bssid: String,
    val ssid: String,
    val minRssi: Int,
    val maxRssi: Int,
    val avgRssi: Double,
    val measurementCount: Int,
)

interface MeasurementRepository {
    fun getMeasurements(surveyId: Long): Flow<List<MeasurementPoint>>
    suspend fun addMeasurement(surveyId: Long, point: MeasurementPoint)
    suspend fun deleteMeasurement(pointId: Long)
    suspend fun getMeasurementSamples(surveyId: Long, bssid: String): List<MeasurementSample>
    suspend fun getBandMeasurementSamples(surveyId: Long, band: WifiBand): List<MeasurementSample>
    suspend fun getApSummary(surveyId: Long): List<ApSummary>
    suspend fun getPointCount(surveyId: Long): Int
}
