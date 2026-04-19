package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.entity.ComposerRequest
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.entity.WorkRequest
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
    private val json: Json,
    private val composersRepository: ComposersRepository
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

    suspend fun createWorkFromOpenOpus(
        openOpusWorkId: String,
        title: String,
        openOpusComposerId: String,
        composerName: String
    ): Work {
        val composer = composersRepository.createComposer(
            ComposerRequest(openOpusId = openOpusComposerId, name = composerName)
        )
        return try {
            apiService().createWork(WorkRequest(openOpusId = openOpusWorkId, title = title, composerIds = listOf(composer.id)))
        } catch (exception: HttpException) {
            if (exception.code() == 409) {
                apiService().getWork(openOpusWorkId)
            } else {
                throw exception
            }
        }
    }
}
