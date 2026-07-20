package com.chaddy50.concerttracker.data.external.dataTransferObjects

import kotlinx.serialization.Serializable

@Serializable
data class FeaturedPerformerDto(
    val performer: PerformerDto,
    val role: String? = null,
    val order: Int = 0
)