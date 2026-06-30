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
    @Query("SELECT * FROM performances WHERE status = 'UPCOMING' ORDER BY date ASC")
    fun observeUpcoming(): Flow<List<PerformanceWithRelations>>

    @Transaction
    @Query("SELECT * FROM performances WHERE status = 'UPCOMING' ORDER BY date ASC LIMIT 1")
    fun observeNextUpcoming(): Flow<PerformanceWithRelations?>

    @Transaction
    @Query("SELECT * FROM performances WHERE status = 'ATTENDED' AND date > :cutoffIso ORDER BY date DESC")
    fun observeRecentlyAttended(cutoffIso: String): Flow<List<PerformanceWithRelations>>

    @Transaction
    @Query(
        "SELECT * FROM performances " +
            "WHERE status IN ('ATTENDED', 'CANCELLED', 'MISSED', 'SKIPPED') " +
            "ORDER BY date DESC"
    )
    fun observePast(): Flow<List<PerformanceWithRelations>>

    @Transaction
    @Query("SELECT * FROM performances WHERE id = :id")
    fun observePerformance(id: String): Flow<PerformanceWithRelations?>

    @Upsert
    suspend fun upsert(performance: PerformanceEntity)

    @Upsert
    suspend fun upsertHeadlinePerformers(crossRefs: List<HeadlinePerformerEntity>)

    @Query("DELETE FROM headline_performers WHERE performanceId = :performanceId")
    suspend fun deleteHeadlinePerformers(performanceId: String)

    @Query("DELETE FROM performances WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM performances")
    suspend fun deleteAll()

    @Query("DELETE FROM performances WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)
}
