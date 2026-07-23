package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chaddy50.concerttracker.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Insert
    suspend fun enqueue(op: SyncOperationEntity): Long

    @Query("SELECT * FROM sync_operations ORDER BY id ASC")
    suspend fun getAllOrdered(): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE id = :id")
    suspend fun getById(id: Long): SyncOperationEntity?

    @Query("SELECT * FROM sync_operations ORDER BY id ASC")
    fun observeAll(): Flow<List<SyncOperationEntity>>

    @Query("SELECT COUNT(*) FROM sync_operations")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_operations WHERE lastError IS NOT NULL")
    fun observeFailedCount(): Flow<Int>

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sync_operations WHERE entityId = :entityLocalId")
    suspend fun deleteForEntity(entityLocalId: String)

    @Query("UPDATE sync_operations SET attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun incrementAttempt(id: Long)

    @Query("UPDATE sync_operations SET lastError = :error WHERE id = :id")
    suspend fun markFailed(id: Long, error: String)

    @Query("UPDATE sync_operations SET attemptCount = 0, lastError = NULL WHERE lastError IS NOT NULL")
    suspend fun resetFailures()
}
