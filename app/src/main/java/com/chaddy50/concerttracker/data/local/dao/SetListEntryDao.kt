package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chaddy50.concerttracker.data.local.entity.SetListEntryEntity
import com.chaddy50.concerttracker.data.local.entity.FeaturedPerformerEntity

@Dao
interface SetListEntryDao {

    @Upsert
    suspend fun upsert(entries: List<SetListEntryEntity>)

    @Upsert
    suspend fun upsertFeaturedPerformers(crossRefs: List<FeaturedPerformerEntity>)

    @Query("SELECT performanceId FROM set_list_entries WHERE id = :entryId")
    suspend fun getPerformanceIdFor(entryId: String): String?

    @Query("DELETE FROM set_list_entries WHERE performanceId = :performanceId")
    suspend fun deleteForPerformance(performanceId: String)

    @Query("DELETE FROM set_list_entries WHERE id = :id")
    suspend fun delete(id: String)
}
