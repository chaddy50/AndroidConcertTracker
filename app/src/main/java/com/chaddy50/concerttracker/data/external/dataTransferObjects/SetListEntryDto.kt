package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.FeaturedPerformer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import kotlinx.serialization.Serializable

@Serializable
data class SetListEntryDto(
    val id: String,
    val work: WorkDto,
    val order: Int,
    val conductor: PerformerDto? = null,
    val featuredPerformers: List<FeaturedPerformerDto> = emptyList(),
    val notes: String? = null
)

fun SetListEntryDto.toDomain() = SetListEntry(
    id = id,
    work = work.toDomain(),
    order = order,
    conductor = conductor?.toDomain(),
    featuredPerformers = featuredPerformers.map {
        FeaturedPerformer(performer = it.performer.toDomain(), role = it.role)
    },
    notes = notes ?: ""
)
