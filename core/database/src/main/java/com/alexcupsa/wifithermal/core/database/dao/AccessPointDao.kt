package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alexcupsa.wifithermal.core.database.entity.AccessPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessPointDao {

    @Query("SELECT * FROM access_points ORDER BY lastSeen DESC")
    fun getAllAccessPoints(): Flow<List<AccessPointEntity>>

    @Query("SELECT * FROM access_points WHERE bssid = :bssid")
    suspend fun getByBssid(bssid: String): AccessPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ap: AccessPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(aps: List<AccessPointEntity>)

    @Update
    suspend fun update(ap: AccessPointEntity)

    @Query("UPDATE access_points SET isFavorite = :favorite WHERE bssid = :bssid")
    suspend fun setFavorite(bssid: String, favorite: Boolean)

    @Query("UPDATE access_points SET notes = :notes WHERE bssid = :bssid")
    suspend fun setNotes(bssid: String, notes: String?)

    @Query("SELECT * FROM access_points WHERE isFavorite = 1 ORDER BY lastSeen DESC")
    fun getFavorites(): Flow<List<AccessPointEntity>>

    @Query("SELECT COUNT(DISTINCT bssid) FROM access_points")
    suspend fun getTotalApCount(): Int
}
