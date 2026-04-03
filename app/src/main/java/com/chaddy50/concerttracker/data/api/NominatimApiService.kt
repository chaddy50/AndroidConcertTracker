package com.chaddy50.concerttracker.data.api

import com.chaddy50.concerttracker.data.entity.NominatimResult
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 10
    ): List<NominatimResult>
}