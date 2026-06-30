package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
import kotlinx.serialization.Serializable

@Serializable
data class PerformerDto(
    val id: String,
    val name: String,
    val type: PerformerType,
    val specialty: String? = null,
    val musicbrainzId: String? = null
)

internal fun PerformerDto.toRow() =
    PerformerEntity(id = id, name = name, type = type.name, specialty = specialty, musicbrainzId = musicbrainzId)

fun PerformerDto.toDomain() =
    Performer(id = id, name = name, type = type, specialty = specialty, musicbrainzId = musicbrainzId)
