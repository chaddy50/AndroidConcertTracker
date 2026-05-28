package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Composer(
    val id: String,
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("open_opus_id") val openOpusId: String? = null
)