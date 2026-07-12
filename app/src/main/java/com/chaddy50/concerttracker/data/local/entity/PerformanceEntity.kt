package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "performances",
    indices = [Index("venueId"), Index("conductorId")]
)
data class PerformanceEntity(
    @PrimaryKey val id: String,
    val date: String,
    /** [com.chaddy50.concerttracker.data.enum.PerformanceStatus] */
    val status: String,
    val venueId: String,
    val conductorId: String? = null,
    /** [com.chaddy50.concerttracker.data.enum.SyncState] */
    val syncState: String = "SYNCED"
)
