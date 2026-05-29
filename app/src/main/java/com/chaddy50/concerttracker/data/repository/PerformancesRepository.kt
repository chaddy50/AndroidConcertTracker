package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.api.PerformanceRequest
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

    suspend fun getNextUpcomingPerformance(): Performance? {
        return apiService().getPerformances(
            status = "UPCOMING",
            sort = "date_asc",
            limit = 1
        ).firstOrNull()
    }

    suspend fun getRecentlyAttendedPerformances(): List<Performance> {
        val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).toString()
        return apiService().getPerformances(
            status = "ATTENDED",
            sort = "date_desc",
            dateAfter = thirtyDaysAgo
        )
    }

    suspend fun getUpcomingPerformances(): List<Performance> {
        return apiService().getPerformances(
            status = "UPCOMING",
            sort = "date_asc"
        )
    }

    suspend fun getPastPerformances(): List<Performance> {
        return apiService().getPerformances(
            status = "ATTENDED,CANCELLED,MISSED,SKIPPED",
            sort = "date_desc"
        )
    }

    suspend fun getPerformance(id: String): Performance = apiService().getPerformance(id)

    suspend fun createPerformance(request: PerformanceRequest): Performance {
        return apiService().createPerformance(request)
    }

    suspend fun updatePerformance(id: String, request: PerformanceRequest): Performance {
        return apiService().updatePerformance(id, request)
    }

    suspend fun deletePerformance(id: String) {
        apiService().deletePerformance(id)
    }
}
