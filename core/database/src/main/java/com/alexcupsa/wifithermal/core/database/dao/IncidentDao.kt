package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alexcupsa.wifithermal.core.database.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 200): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE bssid = :bssid ORDER BY timestamp DESC")
    fun getByBssid(bssid: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE timestamp >= :sinceMs ORDER BY timestamp DESC")
    fun getSince(sinceMs: Long): Flow<List<IncidentEntity>>

    @Insert
    suspend fun insert(incident: IncidentEntity): Long

    @Query("DELETE FROM incidents WHERE timestamp < :beforeMs")
    suspend fun pruneBefore(beforeMs: Long): Int

    @Query("DELETE FROM incidents")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM incidents WHERE timestamp >= :sinceMs")
    fun countSince(sinceMs: Long): Flow<Int>
}
