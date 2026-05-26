package com.chaddy50.concerttracker.data.api

import com.chaddy50.concerttracker.data.entity.Composer
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ConcertTrackerApiService {
    companion object {
        const val API_VERSION = "v1"
    }

    @GET("performances")
    suspend fun getPerformances(): List<Performance>

    @GET("performances/{id}")
    suspend fun getPerformance(@Path("id") id: String): Performance

    @POST("performances/")
    suspend fun createPerformance(@Body request: PerformanceRequest): Performance

    @PUT("performances/{id}")
    suspend fun updatePerformance(@Path("id") id: String, @Body request: PerformanceRequest): Performance

    @POST("set-list-entries/")
    suspend fun createSetListEntry(@Body request: SetListEntryCreateRequest): SetListEntry

    @PUT("set-list-entries/{id}")
    suspend fun updateSetListEntry(@Path("id") id: String, @Body request: SetListEntryRequest): SetListEntry

    @PUT("set-list-entries/{id}")
    suspend fun updateSetListEntryFull(@Path("id") id: String, @Body request: SetListEntryUpdateRequest): SetListEntry

    @DELETE("set-list-entries/{id}")
    suspend fun deleteSetListEntry(@Path("id") id: String)

    @POST("venues/")
    suspend fun createVenue(@Body request: VenueRequest): Venue

    @POST("performers/")
    suspend fun createPerformer(@Body request: PerformerRequest): Performer

    @GET("performers/{id}")
    suspend fun getPerformer(@Path("id") id: String): Performer

    @POST("composers/")
    suspend fun createComposer(@Body request: ComposerRequest): Composer

    @GET("composers/{id}")
    suspend fun getComposer(@Path("id") id: String): Composer

    @POST("works/")
    suspend fun createWork(@Body request: WorkRequest): Work

    @GET("works/{id}")
    suspend fun getWork(@Path("id") id: String): Work
}

@Serializable
data class WorkRequest(
    val title: String,
    @SerialName("open_opus_id") val openOpusId: String? = null,
    val composers: List<ComposerRequest>
)

@Serializable
data class ComposerRequest(
    val name: String,
    @SerialName("open_opus_id") val openOpusId: String? = null
)

@Serializable
data class PerformanceRequest(
    val date: String,
    val venueId: String,
    val performerIds: List<String>,
    val status: PerformanceStatus
)

@Serializable
data class PerformerRequest(
    val name: String,
    val type: PerformerType,
    val specialty: String? = null,
    @SerialName("musicbrainz_id") val musicbrainzId: String? = null
)

@Serializable
data class SetListEntryRequest(
    val notes: String? = null
)

@Serializable
data class FeaturedPerformerRequest(
    val performerId: String,
    val role: String? = null
)

@Serializable
data class SetListEntryCreateRequest(
    val performanceId: String,
    val workId: String,
    val order: Int,
    val featuredPerformers: List<FeaturedPerformerRequest>
)

@Serializable
data class SetListEntryUpdateRequest(
    val workId: String? = null,
    val order: Int? = null,
    val featuredPerformers: List<FeaturedPerformerRequest>? = null
)