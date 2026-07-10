package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueDao {

    @Upsert
    suspend fun upsert(venues: List<VenueEntity>)

    @Query("SELECT * FROM venues WHERE id = :id")
    suspend fun getById(id: String): VenueEntity?

    @Query("SELECT * FROM venues WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchVenues(query: String): Flow<List<VenueEntity>>
}
