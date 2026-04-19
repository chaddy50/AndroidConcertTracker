package com.chaddy50.concerttracker.navigation

import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import kotlinx.serialization.Serializable

@Serializable
object Performances

@Serializable
object Settings

@Serializable
data class PerformanceDetail(val id: String)

@Serializable
data class PerformanceEdit(val id: String?)

@Serializable
object CreateVenue

@Serializable
data class MusicBrainzSearch(val entityType: MusicBrainzEntityType)

@Serializable
data class SetListEntryEdit(val performanceId: String, val entryId: String?)

@Serializable
object OpenOpusWorkSearch