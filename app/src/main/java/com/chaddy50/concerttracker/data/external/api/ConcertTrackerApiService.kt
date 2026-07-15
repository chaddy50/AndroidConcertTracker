package com.chaddy50.concerttracker.data.external.api

import com.chaddy50.concerttracker.data.external.dataTransferObjects.ComposerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.PerformanceDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.PerformerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.SetListEntryDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.VenueDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.WorkDto
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ConcertTrackerApiService {
    companion object {
        const val API_VERSION = "v1"
    }

    @GET("performances")
    suspend fun getPerformances(
        @Query("status") status: String? = null,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("date_after") dateAfter: String? = null
    ): List<PerformanceDto>

    @GET("performances/{id}")
    suspend fun getPerformance(@Path("id") id: String): PerformanceDto

    @POST("performances/")
    suspend fun createPerformance(@Body request: PerformanceRequest): PerformanceDto

    @PUT("performances/{id}")
    suspend fun updatePerformance(@Path("id") id: String, @Body request: PerformanceRequest): PerformanceDto

    @DELETE("performances/{id}")
    suspend fun deletePerformance(@Path("id") id: String)

    @POST("set-list-entries/")
    suspend fun createSetListEntry(@Body request: SetListEntryCreateRequest): SetListEntryDto

    @PUT("set-list-entries/{id}")
    suspend fun updateSetListEntry(@Path("id") id: String, @Body request: SetListEntryUpdateRequest): SetListEntryDto

    @DELETE("set-list-entries/{id}")
    suspend fun deleteSetListEntry(@Path("id") id: String)

    @POST("venues/")
    suspend fun findOrCreateVenue(@Body request: VenueRequest): VenueDto

    @POST("performers/")
    suspend fun findOrCreatePerformer(@Body request: PerformerRequest): PerformerDto

    @GET("performers/{id}")
    suspend fun getPerformer(@Path("id") id: String): PerformerDto

    @POST("composers/")
    suspend fun findOrCreateComposer(@Body request: ComposerRequest): ComposerDto

    @GET("composers/{id}")
    suspend fun getComposer(@Path("id") id: String): ComposerDto

    @POST("works/")
    suspend fun findOrCreateWork(@Body request: WorkRequest): WorkDto

    @GET("works/{id}")
    suspend fun getWork(@Path("id") id: String): WorkDto
}

@Serializable
data class WorkRequest(
    val title: String,
    val openOpusId: String? = null,
    val composers: List<ComposerRequest>,
    val id: String? = null,
    val type: String? = null
)

@Serializable
data class ComposerRequest(
    val name: String,
    val openOpusId: String? = null,
    // When set, attach the work to this already-materialized composer (our id) instead of
    // find-or-creating one by natural key. Used for cached composers with no Open Opus id.
    val id: String? = null,
    val epoch: String? = null
)

@Serializable
data class PerformanceRequest(
    val date: String,
    val venueId: String,
    val performerIds: List<String>,
    val status: PerformanceStatus,
    val setList: List<SetListEntryInlineRequest> = emptyList(),
    val notes: String? = null,
    val id: String? = null
)

@Serializable
data class PerformerRequest(
    val name: String,
    val type: PerformerType,
    val specialty: String? = null,
    val musicbrainzId: String? = null,
    val id: String? = null
)

@Serializable
data class FeaturedPerformerRequest(
    val performerId: String,
    val role: String? = null
)

@Serializable
data class SetListEntryInlineRequest(
    val workId: String,
    val order: Int,
    val featuredPerformers: List<FeaturedPerformerRequest> = emptyList(),
    val id: String? = null
)

@Serializable
data class SetListEntryCreateRequest(
    val performanceId: String,
    val workId: String,
    val order: Int,
    val featuredPerformers: List<FeaturedPerformerRequest>,
    val id: String? = null
)

@Serializable
data class SetListEntryUpdateRequest(
    val workId: String? = null,
    val order: Int? = null,
    val featuredPerformers: List<FeaturedPerformerRequest>? = null,
    val notes: String? = null
)