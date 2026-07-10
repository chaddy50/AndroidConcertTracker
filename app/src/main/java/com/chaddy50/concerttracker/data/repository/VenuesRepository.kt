package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.VenueRequest
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toDomain
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.local.dao.VenueDao
import com.chaddy50.concerttracker.data.local.entity.toDomain
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenuesRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val venueDao: VenueDao
) {

    fun searchVenues(query: String): Flow<List<Venue>> =
        venueDao.searchVenues(query.trim()).map { list -> list.map { it.toDomain() } }

    private var cachedBaseUrl: String? = null
    private var cachedApiService: ConcertTrackerApiService? = null

    private suspend fun apiService(): ConcertTrackerApiService {
        val baseUrl = "${settingsRepository.serverUrl.first().trimEnd('/')}/${ConcertTrackerApiService.API_VERSION}/"
        if (baseUrl != cachedBaseUrl || cachedApiService == null) {
            cachedBaseUrl = baseUrl
            cachedApiService = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ConcertTrackerApiService::class.java)
        }
        return cachedApiService!!
    }

    suspend fun findOrCreateVenue(request: VenueRequest): ApiResult<Venue> = safeApiCall {
        apiService().findOrCreateVenue(request).toDomain()
    }
}
