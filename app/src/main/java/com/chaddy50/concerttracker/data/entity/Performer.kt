package com.chaddy50.concerttracker.data.entity

import com.chaddy50.concerttracker.data.enum.PerformerType
import kotlinx.serialization.Serializable

@Serializable
data class Performer(
    val id: String,
    val name: String,
    val type: PerformerType
)

@Serializable
data class PerformerRequest(
    val id: String,
    val name: String,
    val type: PerformerType
)