package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chaddy50.concerttracker.data.local.entity.VenueEntity

@Dao
interface VenueDao {

    @Upsert
    suspend fun upsert(venues: List<VenueEntity>)

    @Query("SELECT * FROM venues WHERE id = :id")
    suspend fun getById(id: String): VenueEntity?
}
