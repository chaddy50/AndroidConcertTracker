package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.api.PerformanceRequest
import com.chaddy50.concerttracker.data.entity.Performance
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
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

    private val _performances = MutableStateFlow<List<Performance>>(emptyList())
    val performances: StateFlow<List<Performance>> = _performances.asStateFlow()

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

    suspend fun getPerformances(): List<Performance> {
        val result = apiService().getPerformances()
        _performances.value = result
        return result
    }

    suspend fun getPerformance(id: String): Performance = apiService().getPerformance(id)

    suspend fun createPerformance(request: PerformanceRequest): Performance {
        val result = apiService().createPerformance(request)
        _performances.value = _performances.value + result
        return result
    }

    suspend fun updatePerformance(id: String, request: PerformanceRequest): Performance {
        val result = apiService().updatePerformance(id, request)
        _performances.value = _performances.value.map { if (it.id == id) result else it }
        return result
    }
}