package com.chaddy50.concerttracker.data.entity

import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import kotlinx.serialization.Serializable

@Serializable
data class Performance(
    val id: String,
    val date: String,
    val venue: Venue,
    val performers: List<Performer> = emptyList(),
    val conductor: Performer? = null,
    val status: PerformanceStatus,
    val notes: String? = null,
    val setList: List<SetListEntry> = emptyList()
)
