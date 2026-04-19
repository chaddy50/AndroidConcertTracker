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

@Serializable
data class SetListEntryRequest(
    val notes: String? = null
)

@Serializable
data class FeaturedPerformerRequest(
    val performerId: String,
    val role: String? = null
)

@Serializable
data class SetListEntryCreateRequest(
    val performanceId: String,
    val workId: String,
    val order: Int,
    val featuredPerformers: List<FeaturedPerformerRequest>,
    val conductorId: String? = null
)

@Serializable
data class SetListEntryUpdateRequest(
    val workId: String? = null,
    val order: Int? = null,
    val featuredPerformers: List<FeaturedPerformerRequest>? = null,
    val conductorId: String? = null
)
