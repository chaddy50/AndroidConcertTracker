package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import kotlinx.serialization.Serializable

@Serializable
data class VenueDto(
    val id: String,
    val name: String,
    val osmId: String,
    val osmType: String
)

internal fun VenueDto.toRow() = VenueEntity(id = id, name = name, osmId = osmId, osmType = osmType)

fun VenueDto.toDomain() = Venue(id = id, name = name, osmId = osmId, osmType = osmType)
