package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryUpdateRequest
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.local.dao.SetListEntryDao
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toDomain
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetListEntriesRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val setListEntryDao: SetListEntryDao,
    private val performancesRepository: PerformancesRepository
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

    suspend fun createSetListEntry(request: SetListEntryCreateRequest): ApiResult<SetListEntry> {
        val result = safeApiCall { apiService().createSetListEntry(request).toDomain() }
        if (result is ApiResult.Success) performancesRepository.loadPerformance(request.performanceId)
        return result
    }

    suspend fun updateSetListEntry(id: String, request: SetListEntryRequest): ApiResult<SetListEntry> {
        val parentId = setListEntryDao.getPerformanceIdFor(id)
        val result = safeApiCall { apiService().updateSetListEntry(id, request).toDomain() }
        if (result is ApiResult.Success) loadParent(parentId)
        return result
    }

    suspend fun updateSetListEntryFull(id: String, request: SetListEntryUpdateRequest): ApiResult<SetListEntry> {
        val parentId = setListEntryDao.getPerformanceIdFor(id)
        val result = safeApiCall { apiService().updateSetListEntryFull(id, request).toDomain() }
        if (result is ApiResult.Success) loadParent(parentId)
        return result
    }

    suspend fun deleteSetListEntry(id: String): ApiResult<Unit> {
        val parentId = setListEntryDao.getPerformanceIdFor(id)
        val result = safeApiCall { apiService().deleteSetListEntry(id) }
        if (result is ApiResult.Success) loadParent(parentId)
        return result
    }

    /**
     * Best-effort write-through: re-fetch the affected performance into the cache so observers
     * re-emit. The mutation already succeeded server-side; a failed re-fetch is reconciled by the
     * next full [PerformancesRepository.loadPerformances].
     */
    private suspend fun loadParent(parentId: String?) {
        if (parentId != null) {
            performancesRepository.loadPerformance(parentId)
        } else {
            performancesRepository.loadPerformances()
        }
    }
}
