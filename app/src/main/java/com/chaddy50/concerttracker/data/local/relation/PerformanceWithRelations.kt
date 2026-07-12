package com.chaddy50.concerttracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.SyncState
import com.chaddy50.concerttracker.data.local.entity.PerformanceEntity
import com.chaddy50.concerttracker.data.local.entity.HeadlinePerformerEntity
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
import com.chaddy50.concerttracker.data.local.entity.SetListEntryEntity
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import com.chaddy50.concerttracker.data.local.entity.toDomain

data class PerformanceWithRelations(
    @Embedded val performance: PerformanceEntity,
    @Relation(parentColumn = "venueId", entityColumn = "id")
    val venue: VenueEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = HeadlinePerformerEntity::class,
            parentColumn = "performanceId",
            entityColumn = "performerId"
        )
    )
    val performers: List<PerformerEntity>,
    @Relation(parentColumn = "conductorId", entityColumn = "id")
    val conductor: PerformerEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "performanceId",
        entity = SetListEntryEntity::class
    )
    val setList: List<SetListEntryWithRelations>
)

fun PerformanceWithRelations.toDomain(): Performance = Performance(
    id = performance.id,
    date = performance.date,
    venue = requireNotNull(venue) { "Cached performance ${performance.id} is missing its venue" }.toDomain(),
    performers = performers.map { it.toDomain() },
    conductor = conductor?.toDomain(),
    status = PerformanceStatus.valueOf(performance.status),
    setList = setList
        .filter { it.entry.syncState != SyncState.PENDING_DELETE.toName() }
        .map { it.toDomain() }
        .sortedBy { it.order },
    syncState = SyncState.fromName(performance.syncState)
)
