package com.chaddy50.concerttracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.chaddy50.concerttracker.data.domain.FeaturedPerformer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
import com.chaddy50.concerttracker.data.local.entity.SetListEntryEntity
import com.chaddy50.concerttracker.data.local.entity.FeaturedPerformerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import com.chaddy50.concerttracker.data.local.entity.toDomain

/**
 * A featured-performer link together with the performer it points at. The cross-ref carries the
 * [role], which a plain junction relation would drop, so it is embedded explicitly.
 */
data class FeaturedPerformerWithPerformer(
    @Embedded val ref: FeaturedPerformerEntity,
    @Relation(parentColumn = "performerId", entityColumn = "id")
    val performer: PerformerEntity
)

data class SetListEntryWithRelations(
    @Embedded val entry: SetListEntryEntity,
    @Relation(parentColumn = "workId", entityColumn = "id", entity = WorkEntity::class)
    val work: WorkWithComposers,
    @Relation(parentColumn = "conductorId", entityColumn = "id")
    val conductor: PerformerEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "setListEntryId",
        entity = FeaturedPerformerEntity::class
    )
    val featuredPerformers: List<FeaturedPerformerWithPerformer>
)

fun SetListEntryWithRelations.toDomain() = SetListEntry(
    id = entry.id,
    work = work.toDomain(),
    order = entry.order,
    conductor = conductor?.toDomain(),
    featuredPerformers = featuredPerformers.map {
        FeaturedPerformer(performer = it.performer.toDomain(), role = it.ref.role)
    },
    notes = entry.notes
)
