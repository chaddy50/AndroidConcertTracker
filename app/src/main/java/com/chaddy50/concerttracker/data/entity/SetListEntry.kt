package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class SetListEntry(
    val id: String,
    val work: Work,
    val order: Int,
    val conductor: Performer? = null,
    val featuredPerformers: List<SetListEntryPerformer> = emptyList(),
    val notes: String? = null
)

@Serializable
data class SetListEntryPerformer(
    val performer: Performer,
    val role: String? = null
)
