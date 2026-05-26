package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class Work(
    val id: String,
    val title: String,
    val composers: List<Composer> = emptyList()
)