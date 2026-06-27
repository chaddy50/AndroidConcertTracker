package com.chaddy50.concerttracker.data.entity

import com.chaddy50.concerttracker.data.enum.PerformerType
import kotlinx.serialization.Serializable

@Serializable
data class Performer(
    val id: String,
    val name: String,
    val type: PerformerType,
    val specialty: String? = null,
    val musicbrainzId: String? = null
)
