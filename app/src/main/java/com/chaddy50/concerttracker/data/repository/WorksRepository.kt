package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.ComposerRequest
import com.chaddy50.concerttracker.data.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.api.WorkRequest
import com.chaddy50.concerttracker.data.api.safeApiCall
import com.chaddy50.concerttracker.data.entity.Work
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorksRepository @Inject constructor(
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

    suspend fun searchWorks(query: String): ApiResult<List<Work>> = safeApiCall {
        apiService().getWorks(name = query.ifBlank { null })
    }

    suspend fun createWorkFromOpenOpus(
        openOpusWorkId: String,
        title: String,
        openOpusComposerId: String,
        composerName: String
    ): ApiResult<Work> = safeApiCall {
        try {
            apiService().createWork(
                WorkRequest(
                    title = title,
                    openOpusId = openOpusWorkId,
                    composers = listOf(ComposerRequest(name = composerName, openOpusId = openOpusComposerId))
                )
            )
        } catch (e: HttpException) {
            if (e.code() == 409) {
                val body = e.response()?.errorBody()?.string()
                if (body != null) {
                    json.decodeFromString<Work>(body)
                } else {
                    throw e
                }
            } else {
                throw e
            }
        }
    }
}
