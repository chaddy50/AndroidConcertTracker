package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import kotlinx.serialization.Serializable

@Serializable
data class VenueDto(
    val id: String,
    val name: String,
    val osmId: String? = null,
    val osmType: String? = null,
    val formattedAddress: String? = null,
    val city: String? = null,
    val country: String? = null,
    val websiteUri: String? = null
)

internal fun VenueDto.toRow() = VenueEntity(
    id = id,
    name = name,
    osmId = osmId,
    osmType = osmType,
    address = formattedAddress,
    city = city,
    country = country,
    website = websiteUri
)

fun VenueDto.toDomain() = Venue(
    id = id,
    name = name,
    osmId = osmId,
    osmType = osmType,
    address = formattedAddress,
    city = city,
    country = country,
    website = websiteUri
)
