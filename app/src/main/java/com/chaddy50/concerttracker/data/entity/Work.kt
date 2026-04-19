package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class Work(
    val id: String,
    val title: String,
    val composers: List<Composer> = emptyList()
)

@Serializable
data class WorkRequest(
    val openOpusId: String,
    val title: String,
    val composerIds: List<String>
)