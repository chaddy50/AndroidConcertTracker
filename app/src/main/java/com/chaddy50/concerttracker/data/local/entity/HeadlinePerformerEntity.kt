package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "headline_performers",
    primaryKeys = ["performanceId", "performerId"],
    foreignKeys = [
        ForeignKey(
            entity = PerformanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["performanceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("performerId")]
)
data class HeadlinePerformerEntity(
    val performanceId: String,
    val performerId: String
)
