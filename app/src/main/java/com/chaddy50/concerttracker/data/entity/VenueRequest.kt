package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class VenueRequest(
    val osmType: String,
    val osmId: String,
    val name: String
)
