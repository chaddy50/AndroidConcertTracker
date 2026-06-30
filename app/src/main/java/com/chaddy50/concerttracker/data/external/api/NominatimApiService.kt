package com.chaddy50.concerttracker.data.external.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {
    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 10
    ): List<NominatimResult>
}

@Serializable
data class NominatimResult(
    val osmId: Long,
    val osmType: String,
    val displayName: String,
    val name: String = ""
)

@Serializable
data class VenueRequest(
    val osmType: String,
    val osmId: String,
    val name: String
)
