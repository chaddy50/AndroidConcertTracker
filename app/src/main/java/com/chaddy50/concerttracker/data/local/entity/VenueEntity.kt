package com.chaddy50.concerttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chaddy50.concerttracker.data.domain.Venue

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val osmId: String?,
    val osmType: String?,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val website: String? = null
)

internal fun VenueEntity.toDomain() = Venue(
    id = id,
    name = name,
    osmId = osmId,
    osmType = osmType,
    address = address,
    city = city,
    country = country,
    website = website
)
