package com.chaddy50.concerttracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.local.entity.ComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import com.chaddy50.concerttracker.data.local.entity.toDomain

data class WorkWithComposers(
    @Embedded val work: WorkEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorkComposerEntity::class,
            parentColumn = "workId",
            entityColumn = "composerId"
        )
    )
    val composers: List<ComposerEntity>
)

fun WorkWithComposers.toDomain() = Work(
    id = work.id,
    title = work.title,
    composers = composers.map { it.toDomain() },
    openOpusId = work.openOpusId,
    genre = work.genre
)
