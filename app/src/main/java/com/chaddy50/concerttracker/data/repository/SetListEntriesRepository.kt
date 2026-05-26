package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.api.SetListEntryRequest
import com.chaddy50.concerttracker.data.api.SetListEntryUpdateRequest
import com.chaddy50.concerttracker.data.entity.SetListEntry
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

    suspend fun createSetListEntry(request: SetListEntryCreateRequest): SetListEntry =
        apiService().createSetListEntry(request)

    suspend fun updateSetListEntry(id: String, request: SetListEntryRequest): SetListEntry =
        apiService().updateSetListEntry(id, request)

    suspend fun updateSetListEntryFull(id: String, request: SetListEntryUpdateRequest): SetListEntry =
        apiService().updateSetListEntryFull(id, request)

    suspend fun deleteSetListEntry(id: String) =
        apiService().deleteSetListEntry(id)
}