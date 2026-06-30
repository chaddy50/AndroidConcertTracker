package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.local.entity.ComposerEntity
import com.chaddy50.concerttracker.data.local.entity.FeaturedPerformerEntity
import com.chaddy50.concerttracker.data.local.entity.HeadlinePerformerEntity
import com.chaddy50.concerttracker.data.local.entity.PerformanceEntity
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
import com.chaddy50.concerttracker.data.local.entity.SetListEntryEntity
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import com.chaddy50.concerttracker.data.local.entity.WorkComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import kotlinx.serialization.Serializable

@Serializable
data class PerformanceDto(
    val id: String,
    val date: String,
    val venue: VenueDto,
    val performers: List<PerformerDto> = emptyList(),
    val conductor: PerformerDto? = null,
    val status: PerformanceStatus,
    val setList: List<SetListEntryDto> = emptyList()
)

/**
 * The flattened set of Room rows that make up a single performance. The repository persists all of
 * these in one transaction so the cached graph stays consistent.
 */
data class PerformanceRows(
    val performance: PerformanceEntity,
    val venues: List<VenueEntity>,
    val performers: List<PerformerEntity>,
    val works: List<WorkEntity>,
    val composers: List<ComposerEntity>,
    val setListEntries: List<SetListEntryEntity>,
    val headlinePerformers: List<HeadlinePerformerEntity>,
    val workComposers: List<WorkComposerEntity>,
    val featuredPerformers: List<FeaturedPerformerEntity>
)

fun PerformanceDto.toRows(): PerformanceRows {
    val performers = mutableListOf<PerformerDto>()
    val works = mutableListOf<WorkDto>()
    val composers = mutableListOf<ComposerDto>()
    val setListEntries = mutableListOf<SetListEntryEntity>()
    val headlinePerformers = mutableListOf<HeadlinePerformerEntity>()
    val workComposers = mutableListOf<WorkComposerEntity>()
    val featuredPerformers = mutableListOf<FeaturedPerformerEntity>()

    performers.addAll(this.performers)
    conductor?.let { performers.add(it) }

    this.performers.forEach { performer ->
        headlinePerformers.add(HeadlinePerformerEntity(id, performer.id))
    }

    setList.forEach { entry ->
        works.add(entry.work)
        composers.addAll(entry.work.composers)
        entry.work.composers.forEach { composer ->
            workComposers.add(WorkComposerEntity(entry.work.id, composer.id))
        }
        entry.conductor?.let { performers.add(it) }
        entry.featuredPerformers.forEach { featured ->
            performers.add(featured.performer)
            featuredPerformers.add(
                FeaturedPerformerEntity(entry.id, featured.performer.id, featured.role)
            )
        }
        setListEntries.add(
            SetListEntryEntity(
                id = entry.id,
                performanceId = id,
                workId = entry.work.id,
                order = entry.order,
                conductorId = entry.conductor?.id,
                notes = entry.notes
            )
        )
    }

    return PerformanceRows(
        performance = PerformanceEntity(
            id = id,
            date = date,
            status = status.name,
            venueId = venue.id,
            conductorId = conductor?.id
        ),
        venues = listOf(venue.toRow()),
        performers = performers.distinctBy { it.id }.map { it.toRow() },
        works = works.distinctBy { it.id }.map { it.toRow() },
        composers = composers.distinctBy { it.id }.map { it.toRow() },
        setListEntries = setListEntries,
        headlinePerformers = headlinePerformers,
        workComposers = workComposers.distinctBy { it.workId to it.composerId },
        featuredPerformers = featuredPerformers
    )
}

fun PerformanceDto.toDomain(): Performance = Performance(
    id = id,
    date = date,
    venue = venue.toDomain(),
    performers = performers.map { it.toDomain() },
    conductor = conductor?.toDomain(),
    status = status,
    setList = setList.map { it.toDomain() }.sortedBy { it.order }
)
