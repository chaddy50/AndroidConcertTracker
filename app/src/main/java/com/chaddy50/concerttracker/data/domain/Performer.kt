package com.chaddy50.concerttracker.data.domain

import com.chaddy50.concerttracker.data.enum.PerformerType

data class Performer(
    val id: String,
    val name: String,
    val type: PerformerType,
    val specialty: String? = null,
    val musicbrainzId: String? = null
)
