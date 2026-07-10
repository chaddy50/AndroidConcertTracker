package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformerDao {

    @Upsert
    suspend fun upsert(performers: List<PerformerEntity>)

    @Query("SELECT * FROM performers WHERE id = :id")
    suspend fun getById(id: String): PerformerEntity?

    @Query("SELECT * FROM performers WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchPerformers(query: String): Flow<List<PerformerEntity>>
}
