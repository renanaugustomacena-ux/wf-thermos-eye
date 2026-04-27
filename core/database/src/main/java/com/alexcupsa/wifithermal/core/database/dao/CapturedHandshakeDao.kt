package com.alexcupsa.wifithermal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alexcupsa.wifithermal.core.database.entity.CapturedHandshakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedHandshakeDao {

    @Query("SELECT * FROM captured_handshakes ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<CapturedHandshakeEntity>>

    @Query("SELECT * FROM captured_handshakes WHERE bssid = :bssid ORDER BY capturedAt DESC")
    fun observeForBssid(bssid: String): Flow<List<CapturedHandshakeEntity>>

    @Query("SELECT * FROM captured_handshakes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CapturedHandshakeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: CapturedHandshakeEntity): Long

    @Update
    suspend fun update(entry: CapturedHandshakeEntity)

    @Query("UPDATE captured_handshakes SET status = :status, backendJobId = :jobId, crackResult = :result WHERE id = :id")
    suspend fun updateOutcome(id: Long, status: String, jobId: String?, result: String?)

    @Query("DELETE FROM captured_handshakes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM captured_handshakes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM captured_handshakes WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>
}
