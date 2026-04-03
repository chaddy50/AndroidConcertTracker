package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.VenueRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
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
    private val json: Json
) {
    private var cachedBaseUrl: String? = null
    private var cachedApiService: ConcertTrackerApiService? = null

    private suspend fun apiService(): ConcertTrackerApiService {
        val baseUrl = "${settingsRepository.serverUrl.first().trimEnd('/')}/v1/"
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

    suspend fun createVenue(request: VenueRequest): Venue = apiService().createVenue(request)
}