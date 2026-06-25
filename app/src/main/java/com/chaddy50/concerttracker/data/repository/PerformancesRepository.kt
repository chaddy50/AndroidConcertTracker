package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.api.PerformanceRequest
import com.chaddy50.concerttracker.data.api.safeApiCall
import com.chaddy50.concerttracker.data.entity.Performance
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformancesRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
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

    suspend fun getNextUpcomingPerformance(): ApiResult<Performance?> = safeApiCall {
        apiService().getPerformances(status = "UPCOMING", sort = "date_asc", limit = 1).firstOrNull()
    }

    suspend fun getRecentlyAttendedPerformances(): ApiResult<List<Performance>> = safeApiCall {
        val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).toString()
        apiService().getPerformances(status = "ATTENDED", sort = "date_desc", dateAfter = thirtyDaysAgo)
    }

    suspend fun getUpcomingPerformances(): ApiResult<List<Performance>> = safeApiCall {
        apiService().getPerformances(status = "UPCOMING", sort = "date_asc")
    }

    suspend fun getPastPerformances(): ApiResult<List<Performance>> = safeApiCall {
        apiService().getPerformances(status = "ATTENDED,CANCELLED,MISSED,SKIPPED", sort = "date_desc")
    }

    suspend fun getPerformance(id: String): ApiResult<Performance> = safeApiCall {
        apiService().getPerformance(id)
    }

    suspend fun createPerformance(request: PerformanceRequest): ApiResult<Performance> = safeApiCall {
        apiService().createPerformance(request)
    }

    suspend fun updatePerformance(id: String, request: PerformanceRequest): ApiResult<Performance> = safeApiCall {
        apiService().updatePerformance(id, request)
    }

    suspend fun deletePerformance(id: String): ApiResult<Unit> = safeApiCall {
        apiService().deletePerformance(id)
    }
}
