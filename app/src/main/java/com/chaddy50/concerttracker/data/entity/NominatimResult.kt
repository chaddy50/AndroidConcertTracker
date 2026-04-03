package com.chaddy50.concerttracker.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimResult(
    @SerialName("osm_id") val osmId: Long,
    @SerialName("osm_type") val osmType: String,
    @SerialName("display_name") val displayName: String,
    val name: String = ""
)
