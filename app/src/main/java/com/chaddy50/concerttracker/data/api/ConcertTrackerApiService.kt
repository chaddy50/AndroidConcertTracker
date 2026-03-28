package com.chaddy50.concerttracker.data.api

import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.PerformanceRequest
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.SetListEntryRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ConcertTrackerApiService {
    @GET("performances")
    suspend fun getPerformances(): List<Performance>

    @GET("performances/{id}")
    suspend fun getPerformance(@Path("id") id: String): Performance

    @POST("performances")
    suspend fun createPerformance(@Body request: PerformanceRequest): Performance

    @PUT("performances/{id}")
    suspend fun updatePerformance(@Path("id") id: String, @Body request: PerformanceRequest): Performance

    @PUT("set-list-entries/{id}")
    suspend fun updateSetListEntry(@Path("id") id: String, @Body request: SetListEntryRequest): SetListEntry
}