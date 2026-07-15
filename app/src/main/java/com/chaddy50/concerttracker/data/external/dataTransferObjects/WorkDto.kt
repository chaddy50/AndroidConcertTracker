package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import kotlinx.serialization.Serializable

@Serializable
data class WorkDto(
    val id: String,
    val title: String,
    val composers: List<ComposerDto> = emptyList(),
    val openOpusId: String? = null,
    val type: String? = null
)

internal fun WorkDto.toRow() = WorkEntity(id = id, title = title, openOpusId = openOpusId, genre = type)

fun WorkDto.toDomain() = Work(
    id = id,
    title = title,
    composers = composers.map { it.toDomain() },
    openOpusId = openOpusId,
    genre = type
)
