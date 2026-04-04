package com.chaddy50.concerttracker.data.api

import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.PerformanceRequest
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.PerformerRequest
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.SetListEntryRequest
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.VenueRequest
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

    @POST("venues")
    suspend fun createVenue(@Body request: VenueRequest): Venue

    @POST("performers")
    suspend fun createPerformer(@Body request: PerformerRequest): Performer

    @GET("performers/{id}")
    suspend fun getPerformer(@Path("id") id: String): Performer
}