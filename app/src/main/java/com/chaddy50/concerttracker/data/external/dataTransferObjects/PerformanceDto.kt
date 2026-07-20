package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.SyncState
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
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
    val setList: List<SetListEntryDto> = emptyList(),
    val notes: String? = null
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

    this.performers.forEachIndexed { index, performer ->
        headlinePerformers.add(HeadlinePerformerEntity(id, performer.id, order = index))
    }

    setList.forEach { entry ->
        works.add(entry.work)
        composers.addAll(entry.work.composers)
        entry.work.composers.forEach { composer ->
            workComposers.add(WorkComposerEntity(entry.work.id, composer.id))
        }
        entry.conductor?.let { performers.add(it) }
        entry.featuredPerformers.forEachIndexed { index, featured ->
            performers.add(featured.performer)
            featuredPerformers.add(
                FeaturedPerformerEntity(entry.id, featured.performer.id, featured.role, order = index)
            )
        }
        setListEntries.add(
            SetListEntryEntity(
                id = entry.id,
                performanceId = id,
                workId = entry.work.id,
                order = entry.order,
                conductorId = entry.conductor?.id,
                notes = entry.notes ?: ""
            )
        )
    }

    return PerformanceRows(
        performance = PerformanceEntity(
            id = id,
            date = date,
            status = status.name,
            venueId = venue.id,
            conductorId = conductor?.id,
            notes = notes ?: ""
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

/** Stamp a client UUID on the performance and each inline set-list entry (offline create). */
fun PerformanceRequest.withClientIds(): PerformanceRequest = copy(
    id = id ?: java.util.UUID.randomUUID().toString(),
    setList = setList.map { it.copy(id = it.id ?: java.util.UUID.randomUUID().toString()) }
)

/**
 * Assemble PENDING Room rows for an offline performance create. The referenced venue/performers/works
 * are assumed already cached, so only the performance-owned rows (performance, headline performers,
 * set-list entries, featured performers) are produced. Call [withClientIds] first so ids are set.
 */
fun PerformanceRequest.toPendingRows(): PerformanceRows {
    val performanceId = requireNotNull(id) { "call withClientIds() before toPendingRows()" }
    val headlinePerformers = performerIds.mapIndexed { index, id -> HeadlinePerformerEntity(performanceId, id, order = index) }
    val setListEntries = mutableListOf<SetListEntryEntity>()
    val featuredPerformers = mutableListOf<FeaturedPerformerEntity>()
    setList.forEach { inline ->
        val entryId = requireNotNull(inline.id) { "call withClientIds() before toPendingRows()" }
        setListEntries.add(
            SetListEntryEntity(
                id = entryId,
                performanceId = performanceId,
                workId = inline.workId,
                order = inline.order,
                syncState = SyncState.PENDING.toName()
            )
        )
        inline.featuredPerformers.forEachIndexed { index, featured ->
            featuredPerformers.add(FeaturedPerformerEntity(entryId, featured.performerId, featured.role, order = index))
        }
    }
    return PerformanceRows(
        performance = PerformanceEntity(
            id = performanceId,
            date = date,
            status = status.name,
            venueId = venueId,
            syncState = SyncState.PENDING.toName()
        ),
        venues = emptyList(),
        performers = emptyList(),
        works = emptyList(),
        composers = emptyList(),
        setListEntries = setListEntries,
        headlinePerformers = headlinePerformers,
        workComposers = emptyList(),
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
    setList = setList.map { it.toDomain() }.sortedBy { it.order },
    notes = notes ?: ""
)
