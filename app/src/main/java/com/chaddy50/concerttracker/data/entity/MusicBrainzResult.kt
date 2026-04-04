package com.chaddy50.concerttracker.data.entity

import com.chaddy50.concerttracker.data.enum.PerformerType

data class MusicBrainzResult(
    val id: String,
    val name: String,
    val description: String? = null,
    val performerType: PerformerType? = null
)
