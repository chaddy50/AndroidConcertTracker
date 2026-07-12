package com.chaddy50.concerttracker.data.repository

import androidx.room.withTransaction
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.PerformerRequest
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.external.dataTransferObjects.PerformerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toDomain
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.dao.PerformerDao
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
import com.chaddy50.concerttracker.data.local.entity.toDomain
import com.chaddy50.concerttracker.data.sync.SyncScheduler
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PerformersRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val performerDao: PerformerDao,
    private val database: ConcertTrackerDatabase,
    private val syncOperationsRepository: SyncOperationsRepository,
    private val syncScheduler: Provider<SyncScheduler>
) {

    fun searchPerformers(query: String): Flow<List<Performer>> =
        performerDao.searchPerformers(query.trim()).map { list -> list.map { it.toDomain() } }

    suspend fun getPerformer(id: String): Performer? = performerDao.getById(id)?.toDomain()

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

    suspend fun findOrCreatePerformer(request: PerformerRequest): ApiResult<Performer> {
        if (request.musicbrainzId != null) return findOrCreatePerformerOnline(request)

        val id = request.id ?: UUID.randomUUID().toString()
        database.withTransaction {
            performerDao.upsert(
                listOf(PerformerEntity(id, request.name, request.type.name, request.specialty, null))
            )
            syncOperationsRepository.enqueue(
                SyncEntityType.PERFORMER, SyncOperationType.CREATE, id, json.encodeToString(request.copy(id = id))
            )
        }
        syncScheduler.get().requestSync()
        return ApiResult.Success(Performer(id, request.name, request.type, request.specialty, null))
    }

    private suspend fun findOrCreatePerformerOnline(request: PerformerRequest): ApiResult<Performer> = safeApiCall {
        val dto = try {
            apiService().findOrCreatePerformer(request)
        } catch (e: HttpException) {
            if (e.code() == 409) {
                val body = e.response()?.errorBody()?.string()
                if (body != null) json.decodeFromString<PerformerDto>(body) else throw e
            } else {
                throw e
            }
        }
        dto.toDomain()
    }
}
