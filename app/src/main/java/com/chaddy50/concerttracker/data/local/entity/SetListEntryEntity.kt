package com.chaddy50.concerttracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_list_entries",
    foreignKeys = [
        ForeignKey(
            entity = PerformanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["performanceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("performanceId"), Index("workId"), Index("conductorId")]
)
data class SetListEntryEntity(
    @PrimaryKey val id: String,
    val performanceId: String,
    val workId: String,
    @ColumnInfo(name = "sort_order") val order: Int,
    val conductorId: String? = null,
    val notes: String? = null
)
