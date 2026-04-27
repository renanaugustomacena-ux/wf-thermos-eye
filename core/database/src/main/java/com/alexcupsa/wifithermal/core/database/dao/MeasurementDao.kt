package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.alexcupsa.wifithermal.core.database.entity.ApMeasurementEntity
import com.alexcupsa.wifithermal.core.database.entity.MeasurementPointEntity
import com.alexcupsa.wifithermal.core.database.relation.MeasurementWithAps
import kotlinx.coroutines.flow.Flow

data class MeasurementSampleProjection(
    val x: Double,
    val y: Double,
    val rssi: Double,
)

data class ApSummaryProjection(
    val bssid: String,
    val ssid: String,
    val minRssi: Int,
    val maxRssi: Int,
    val avgRssi: Double,
    val measurementCount: Int,
)

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurement_points WHERE surveyId = :surveyId ORDER BY timestamp ASC")
    fun getMeasurementPoints(surveyId: Long): Flow<List<MeasurementPointEntity>>

    @Query("SELECT * FROM ap_measurements WHERE measurementPointId = :pointId")
    suspend fun getApMeasurements(pointId: Long): List<ApMeasurementEntity>

    @Transaction
    @Query("SELECT * FROM measurement_points WHERE surveyId = :surveyId")
    fun getMeasurementsWithAps(surveyId: Long): Flow<List<MeasurementWithAps>>

    @Insert
    suspend fun insertMeasurementPoint(point: MeasurementPointEntity): Long

    @Insert
    suspend fun insertApMeasurements(measurements: List<ApMeasurementEntity>)

    @Transaction
    suspend fun insertMeasurementWithAps(
        point: MeasurementPointEntity,
        apMeasurements: List<ApMeasurementEntity>,
    ): Long {
        val pointId = insertMeasurementPoint(point)
        if (apMeasurements.isNotEmpty()) {
            insertApMeasurements(apMeasurements.map { it.copy(measurementPointId = pointId) })
        }
        return pointId
    }

    @Query("DELETE FROM measurement_points WHERE id = :id")
    suspend fun deleteMeasurementPoint(id: Long)

    @Query("""
        SELECT DISTINCT am.bssid, am.ssid,
               MIN(am.rssi) as minRssi, MAX(am.rssi) as maxRssi, AVG(am.rssi) as avgRssi,
               COUNT(*) as measurementCount
        FROM ap_measurements am
        INNER JOIN measurement_points mp ON am.measurementPointId = mp.id
        WHERE mp.surveyId = :surveyId
        GROUP BY am.bssid
        ORDER BY avgRssi DESC
    """)
    suspend fun getApSummary(surveyId: Long): List<ApSummaryProjection>

    @Query("""
        SELECT mp.x, mp.y, am.smoothedRssi as rssi
        FROM measurement_points mp
        INNER JOIN ap_measurements am ON am.measurementPointId = mp.id
        WHERE mp.surveyId = :surveyId AND am.bssid = :bssid
    """)
    suspend fun getMeasurementSamples(surveyId: Long, bssid: String): List<MeasurementSampleProjection>

    @Query("""
        SELECT mp.x, mp.y, MAX(am.smoothedRssi) as rssi
        FROM measurement_points mp
        INNER JOIN ap_measurements am ON am.measurementPointId = mp.id
        WHERE mp.surveyId = :surveyId AND am.band = :band
        GROUP BY mp.id
    """)
    suspend fun getBandMeasurementSamples(surveyId: Long, band: String): List<MeasurementSampleProjection>

    @Query("SELECT COUNT(*) FROM measurement_points WHERE surveyId = :surveyId")
    suspend fun getPointCount(surveyId: Long): Int

    @Query(
        """
        SELECT COUNT(DISTINCT am.bssid)
        FROM ap_measurements am
        INNER JOIN measurement_points mp ON am.measurementPointId = mp.id
        WHERE mp.surveyId = :surveyId
        """,
    )
    suspend fun getDistinctApCount(surveyId: Long): Int
}
