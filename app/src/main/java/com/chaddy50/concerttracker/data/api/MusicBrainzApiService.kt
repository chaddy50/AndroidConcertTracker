package com.chaddy50.concerttracker.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicBrainzApiService {
    @GET("artist")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 10
    ): MusicBrainzArtistResponse

    @GET("work")
    suspend fun searchWorks(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 10
    ): MusicBrainzWorkResponse
}

@Serializable
data class MusicBrainzArtistResponse(
    val artists: List<MusicBrainzArtist> = emptyList()
)

@Serializable
data class MusicBrainzArtist(
    val id: String,
    val name: String,
    val disambiguation: String? = null,
    val type: String? = null
)

@Serializable
data class MusicBrainzWorkResponse(
    val works: List<MusicBrainzWork> = emptyList()
)

@Serializable
data class MusicBrainzWork(
    val id: String,
    val title: String,
    @SerialName("disambiguation") val disambiguation: String? = null
)
