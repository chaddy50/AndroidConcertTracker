package com.chaddy50.concerttracker.data.entity

import com.chaddy50.concerttracker.data.enum.PerformerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Performer(
    val id: String,
    val name: String,
    val type: PerformerType,
    val specialty: String? = null,
    @SerialName("musicbrainz_id") val musicbrainzId: String? = null
)

@Serializable
data class PerformerRequest(
    val name: String,
    val type: PerformerType,
    val specialty: String? = null,
    @SerialName("musicbrainz_id") val musicbrainzId: String? = null
)