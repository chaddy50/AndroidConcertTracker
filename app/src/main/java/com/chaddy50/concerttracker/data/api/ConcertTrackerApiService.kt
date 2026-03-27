package com.chaddy50.concerttracker.data.api

import com.chaddy50.concerttracker.data.entity.Performance
import retrofit2.http.GET
import retrofit2.http.Path

interface ConcertTrackerApiService {
    @GET("performances")
    suspend fun getPerformances(): List<Performance>

    @GET("performances/{id}")
    suspend fun getPerformance(@Path("id") id: String): Performance
}