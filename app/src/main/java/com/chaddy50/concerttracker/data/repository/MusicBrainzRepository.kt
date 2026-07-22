package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.MusicBrainzApiService
import com.chaddy50.concerttracker.data.external.api.MusicBrainzResult
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.enum.PerformerType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicBrainzRepository @Inject constructor(
    private val musicBrainzApiService: MusicBrainzApiService
) {
    suspend fun search(query: String): ApiResult<List<MusicBrainzResult>> = safeApiCall {
        musicBrainzApiService.searchArtists(query).artists.map { artist ->
            MusicBrainzResult(
                id = artist.id,
                name = artist.name,
                description = artist.disambiguation,
                performerType = mapArtistType(artist.type)
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
