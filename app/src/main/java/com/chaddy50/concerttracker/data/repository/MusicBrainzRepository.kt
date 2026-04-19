package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.MusicBrainzApiService
import com.chaddy50.concerttracker.data.entity.MusicBrainzResult
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.data.enum.PerformerType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicBrainzRepository @Inject constructor(
    private val musicBrainzApiService: MusicBrainzApiService
) {
    suspend fun search(
        type: MusicBrainzEntityType,
        query: String
    ): List<MusicBrainzResult> {
        return when (type) {
            MusicBrainzEntityType.PERFORMER -> searchArtists(query, forcedType = null)
            MusicBrainzEntityType.CONDUCTOR -> searchArtists(query, forcedType = PerformerType.CONDUCTOR)
            MusicBrainzEntityType.COMPOSER -> searchArtists(query, forcedType = null)
        }
    }

    private suspend fun searchArtists(query: String, forcedType: PerformerType?): List<MusicBrainzResult> {
        return musicBrainzApiService.searchArtists(query).artists.map { artist ->
            MusicBrainzResult(
                id = artist.id,
                name = artist.name,
                description = artist.disambiguation,
                performerType = forcedType ?: mapArtistType(artist.type)
            )
        }
    }

    private fun mapArtistType(type: String?): PerformerType = when (type) {
        "Orchestra" -> PerformerType.ORCHESTRA
        "Choir" -> PerformerType.CHORUS
        "Group" -> PerformerType.ENSEMBLE
        "Person" -> PerformerType.SOLO
        else -> PerformerType.OTHER
    }
}
