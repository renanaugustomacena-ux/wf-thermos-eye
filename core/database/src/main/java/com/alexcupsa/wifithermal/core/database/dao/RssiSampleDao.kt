package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alexcupsa.wifithermal.core.database.entity.RssiSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RssiSampleDao {

    @Insert
    suspend fun insert(sample: RssiSampleEntity): Long

    @Insert
    suspend fun insertAll(samples: List<RssiSampleEntity>)

    @Query("SELECT * FROM rssi_samples WHERE bssid = :bssid ORDER BY timestamp DESC LIMIT :limit")
    suspend fun forBssid(bssid: String, limit: Int = 200): List<RssiSampleEntity>

    @Query("SELECT * FROM rssi_samples WHERE bssid = :bssid AND timestamp >= :sinceMs ORDER BY timestamp DESC")
    fun forBssidSince(bssid: String, sinceMs: Long): Flow<List<RssiSampleEntity>>

    @Query("SELECT COUNT(*) FROM rssi_samples WHERE bssid = :bssid")
    suspend fun countForBssid(bssid: String): Int

    @Query("DELETE FROM rssi_samples WHERE timestamp < :beforeMs")
    suspend fun pruneBefore(beforeMs: Long): Int

    @Query("DELETE FROM rssi_samples WHERE bssid = :bssid")
    suspend fun deleteForBssid(bssid: String)

    @Query("DELETE FROM rssi_samples")
    suspend fun deleteAll()
}
