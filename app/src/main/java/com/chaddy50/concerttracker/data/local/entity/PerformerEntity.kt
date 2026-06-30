package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.PerformerType

@Entity(tableName = "performers")
data class PerformerEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** [com.chaddy50.concerttracker.data.enum.PerformerType] name; stored as text (see PerformanceEntity). */
    val type: String,
    val specialty: String? = null,
    val musicbrainzId: String? = null
)

internal fun PerformerEntity.toDomain() =
    Performer(id = id, name = name, type = PerformerType.valueOf(type), specialty = specialty, musicbrainzId = musicbrainzId)
