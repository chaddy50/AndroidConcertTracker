package com.chaddy50.concerttracker.data.domain

import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.SyncState

data class Performance(
    val id: String,
    val date: String,
    val venue: Venue,
    val performers: List<Performer> = emptyList(),
    val conductor: Performer? = null,
    val status: PerformanceStatus,
    val setList: List<SetListEntry> = emptyList(),
    val notes: String = "",
    val syncState: SyncState = SyncState.SYNCED
)
