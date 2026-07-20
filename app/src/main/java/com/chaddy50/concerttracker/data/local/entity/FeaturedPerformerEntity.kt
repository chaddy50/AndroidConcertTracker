package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "featured_performers",
    primaryKeys = ["setListEntryId", "performerId"],
    foreignKeys = [
        ForeignKey(
            entity = SetListEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["setListEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("performerId")]
)
data class FeaturedPerformerEntity(
    val setListEntryId: String,
    val performerId: String,
    val role: String? = null,
    val order: Int = 0
)
