package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ComposerRequest
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.WorkRequest
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.external.dataTransferObjects.WorkDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toDomain
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.local.dao.WorkDao
import com.chaddy50.concerttracker.data.local.relation.toDomain
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val workDao: WorkDao
) {

    fun searchWorksForComposer(composerId: String, query: String): Flow<List<Work>> =
        workDao.searchWorksForComposer(composerId, query.trim()).map { list -> list.map { it.toDomain() } }

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

    suspend fun findOrCreateWork(
        openOpusWorkId: String?,
        title: String,
        existingComposerId: String?,
        openOpusComposerId: String?,
        composerName: String
    ): ApiResult<Work> = safeApiCall {
        val dto = try {
            apiService().findOrCreateWork(
                WorkRequest(
                    title = title,
                    openOpusId = openOpusWorkId,
                    composers = listOf(
                        ComposerRequest(
                            name = composerName,
                            openOpusId = openOpusComposerId,
                            id = existingComposerId
                        )
                    )
                )
            )
        } catch (e: HttpException) {
            if (e.code() == 409) {
                val body = e.response()?.errorBody()?.string()
                if (body != null) json.decodeFromString<WorkDto>(body) else throw e
            } else {
                throw e
            }
        }
        dto.toDomain()
    }
}
