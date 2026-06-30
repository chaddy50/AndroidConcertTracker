package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.local.entity.ComposerEntity
import kotlinx.serialization.Serializable

@Serializable
data class ComposerDto(
    val id: String,
    val name: String,
    val sortName: String? = null,
    val openOpusId: String? = null
)

internal fun ComposerDto.toRow() =
    ComposerEntity(id = id, name = name, sortName = sortName, openOpusId = openOpusId)

fun ComposerDto.toDomain() =
    Composer(id = id, name = name, sortName = sortName, openOpusId = openOpusId)
