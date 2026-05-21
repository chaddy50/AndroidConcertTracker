package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Work(
    val id: String,
    val title: String,
    val composers: List<Composer> = emptyList()
)

@Serializable
data class WorkRequest(
    val title: String,
    @SerialName("open_opus_id") val openOpusId: String? = null,
    val composers: List<ComposerRequest>
)