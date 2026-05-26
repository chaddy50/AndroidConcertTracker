package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class FeaturedPerformer(
    val performer: Performer,
    val role: String? = null
)