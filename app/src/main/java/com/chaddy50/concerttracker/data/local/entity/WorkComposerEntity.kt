package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "work_composers",
    primaryKeys = ["workId", "composerId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["workId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("composerId")]
)
data class WorkComposerEntity(
    val workId: String,
    val composerId: String
)
