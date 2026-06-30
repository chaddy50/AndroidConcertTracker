package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import kotlinx.serialization.Serializable

@Serializable
data class WorkDto(
    val id: String,
    val title: String,
    val composers: List<ComposerDto> = emptyList()
)

internal fun WorkDto.toRow() = WorkEntity(id = id, title = title)

fun WorkDto.toDomain() = Work(id = id, title = title, composers = composers.map { it.toDomain() })
