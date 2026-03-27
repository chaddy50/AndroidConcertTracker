package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class Composer(
    val id: String,
    val name: String,
    val shortName: String? = null
)