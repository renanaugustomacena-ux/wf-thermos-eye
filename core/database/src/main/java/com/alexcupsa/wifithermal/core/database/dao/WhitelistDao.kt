package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alexcupsa.wifithermal.core.database.entity.AuthorizedApEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Query("SELECT * FROM authorized_aps ORDER BY ssid ASC, bssid ASC")
    fun getAll(): Flow<List<AuthorizedApEntity>>

    @Query("SELECT * FROM authorized_aps WHERE bssid = :bssid LIMIT 1")
    suspend fun getByBssid(bssid: String): AuthorizedApEntity?

    @Query("SELECT * FROM authorized_aps WHERE ssid = :ssid")
    suspend fun getBySsid(ssid: String): List<AuthorizedApEntity>

    @Query("SELECT bssid FROM authorized_aps")
    suspend fun getAllBssids(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AuthorizedApEntity)

    @Update
    suspend fun update(entry: AuthorizedApEntity)

    @Query("DELETE FROM authorized_aps WHERE bssid = :bssid")
    suspend fun deleteByBssid(bssid: String)

    @Query("DELETE FROM authorized_aps")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM authorized_aps")
    fun count(): Flow<Int>
}
