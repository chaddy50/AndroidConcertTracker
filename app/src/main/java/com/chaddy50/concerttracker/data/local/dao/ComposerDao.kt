package com.chaddy50.concerttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chaddy50.concerttracker.data.local.entity.ComposerEntity

@Dao
interface ComposerDao {

    @Upsert
    suspend fun upsert(composers: List<ComposerEntity>)

    @Query("SELECT * FROM composers WHERE id = :id")
    suspend fun getById(id: String): ComposerEntity?
}
