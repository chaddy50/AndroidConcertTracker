package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chaddy50.concerttracker.data.local.entity.PerformanceEntity
import com.chaddy50.concerttracker.data.local.entity.HeadlinePerformerEntity
import com.chaddy50.concerttracker.data.local.relation.PerformanceWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceDao {

    @Transaction
    @Query("SELECT * FROM performances WHERE syncState != 'PENDING_DELETE' AND date >= :nowIso ORDER BY date ASC")
    fun observeUpcoming(nowIso: String): Flow<List<PerformanceWithRelations>>

    @Transaction
    @Query("SELECT * FROM performances WHERE syncState != 'PENDING_DELETE' AND date >= :nowIso ORDER BY date ASC LIMIT 1")
    fun observeNextUpcoming(nowIso: String): Flow<PerformanceWithRelations?>

    @Transaction
    @Query("SELECT * FROM performances WHERE syncState != 'PENDING_DELETE' AND date > :cutoffIso AND date < :nowIso ORDER BY date DESC")
    fun observeRecentlyAttended(cutoffIso: String, nowIso: String): Flow<List<PerformanceWithRelations>>

    @Transaction
    @Query("SELECT * FROM performances WHERE syncState != 'PENDING_DELETE' AND date < :nowIso ORDER BY date DESC")
    fun observePast(nowIso: String): Flow<List<PerformanceWithRelations>>

    @Transaction
    @Query("SELECT * FROM performances WHERE id = :id AND syncState != 'PENDING_DELETE'")
    fun observePerformance(id: String): Flow<PerformanceWithRelations?>

    @Query("SELECT * FROM performances WHERE id = :id")
    suspend fun getById(id: String): PerformanceEntity?

    @Query("UPDATE performances SET syncState = :syncState WHERE id = :id")
    suspend fun markSyncState(id: String, syncState: String)

    @Upsert
    suspend fun upsert(performance: PerformanceEntity)

    @Upsert
    suspend fun upsertHeadlinePerformers(crossRefs: List<HeadlinePerformerEntity>)

    @Query("DELETE FROM headline_performers WHERE performanceId = :performanceId")
    suspend fun deleteHeadlinePerformers(performanceId: String)

    @Query("DELETE FROM performances WHERE id = :id")
    suspend fun delete(id: String)

    /** Reconcile prune: only ever removes SYNCED rows, so locally-unsynced work is never wiped. */
    @Query("DELETE FROM performances WHERE syncState = 'SYNCED'")
    suspend fun deleteAllSynced()

    @Query("DELETE FROM performances WHERE syncState = 'SYNCED' AND id NOT IN (:ids)")
    suspend fun deleteSyncedNotIn(ids: List<String>)
}
