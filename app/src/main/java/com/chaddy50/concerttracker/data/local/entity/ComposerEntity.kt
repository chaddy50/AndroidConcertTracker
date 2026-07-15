package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chaddy50.concerttracker.data.domain.Composer

@Entity(tableName = "composers")
data class ComposerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortName: String? = null,
    val openOpusId: String? = null,
    val epoch: String? = null
)

internal fun ComposerEntity.toDomain() =
    Composer(id = id, name = name, sortName = sortName, openOpusId = openOpusId, epoch = epoch)
