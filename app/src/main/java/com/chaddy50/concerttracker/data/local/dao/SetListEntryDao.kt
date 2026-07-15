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

    @Query("SELECT * FROM set_list_entries WHERE id = :id")
    suspend fun getById(id: String): SetListEntryEntity?

    @Query("UPDATE set_list_entries SET notes = :notes, syncState = :syncState WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String, syncState: String)

    @Query("UPDATE set_list_entries SET syncState = :syncState WHERE id = :id")
    suspend fun markSyncState(id: String, syncState: String)

    @Query("SELECT * FROM featured_performers WHERE setListEntryId = :setListEntryId")
    suspend fun getFeaturedPerformers(setListEntryId: String): List<FeaturedPerformerEntity>

    @Query("DELETE FROM featured_performers WHERE setListEntryId = :setListEntryId")
    suspend fun deleteFeaturedPerformers(setListEntryId: String)

    @Query("DELETE FROM set_list_entries WHERE performanceId = :performanceId")
    suspend fun deleteForPerformance(performanceId: String)

    @Query("DELETE FROM set_list_entries WHERE id = :id")
    suspend fun delete(id: String)
}
