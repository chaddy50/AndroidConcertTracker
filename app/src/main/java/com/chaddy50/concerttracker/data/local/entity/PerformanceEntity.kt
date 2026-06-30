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
    /** [com.chaddy50.concerttracker.data.enum.PerformanceStatus] name; stored as text so Room never
     * references the `data.enum` package, which Java codegen cannot import. */
    val status: String,
    val venueId: String,
    val conductorId: String? = null
)
