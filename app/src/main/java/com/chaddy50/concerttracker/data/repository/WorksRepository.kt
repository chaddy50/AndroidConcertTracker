package com.chaddy50.concerttracker.data.repository

import androidx.room.withTransaction
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ComposerRequest
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.WorkRequest
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.external.dataTransferObjects.ComposerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.WorkDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toDomain
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toRow
import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.dao.WorkDao
import com.chaddy50.concerttracker.data.local.entity.WorkComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import com.chaddy50.concerttracker.data.local.relation.toDomain
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
class WorksRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val workDao: WorkDao,
    private val composersRepository: ComposersRepository,
    private val database: ConcertTrackerDatabase,
    private val syncOperationsRepository: SyncOperationsRepository,
    private val syncScheduler: Provider<SyncScheduler>
) {

    fun searchWorksForComposer(composerId: String, query: String): Flow<List<Work>> =
        workDao.searchWorksForComposer(composerId, query.trim()).map { list -> list.map { it.toDomain() } }

    suspend fun getWork(id: String): Work? = workDao.getWorkWithComposers(id)?.toDomain()

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
        composerName: String,
        workGenre: String? = null,
        composerEpoch: String? = null
    ): ApiResult<Work> {
        val isCustomWork = openOpusWorkId == null
        val composerNeedsNetwork = existingComposerId == null && openOpusComposerId != null
        if (!isCustomWork || composerNeedsNetwork) {
            return findOrCreateWorkOnline(
                openOpusWorkId, title, workGenre, existingComposerId, openOpusComposerId, composerName, composerEpoch
            )
        }

        val workId = UUID.randomUUID().toString()
        val composerId = existingComposerId ?: UUID.randomUUID().toString()
        database.withTransaction {
            if (existingComposerId == null) {
                composersRepository.upsert(
                    listOf(ComposerDto(id = composerId, name = composerName, openOpusId = openOpusComposerId, epoch = composerEpoch))
                )
            }
            workDao.upsert(listOf(WorkEntity(workId, title, openOpusId = null, genre = workGenre)))
            workDao.upsertWorkComposers(listOf(WorkComposerEntity(workId, composerId)))
            val payload = WorkRequest(
                id = workId,
                title = title,
                openOpusId = null,
                type = workGenre,
                composers = listOf(
                    ComposerRequest(id = composerId, name = composerName, openOpusId = openOpusComposerId, epoch = composerEpoch)
                )
            )
            syncOperationsRepository.enqueue(
                SyncEntityType.WORK, SyncOperationType.CREATE, workId, json.encodeToString(payload)
            )
        }
        syncScheduler.get().requestSync()
        val composer = Composer(id = composerId, name = composerName, openOpusId = openOpusComposerId, epoch = composerEpoch)
        return ApiResult.Success(
            Work(id = workId, title = title, composers = listOf(composer), openOpusId = null, genre = workGenre)
        )
    }

    private suspend fun findOrCreateWorkOnline(
        openOpusWorkId: String?,
        title: String,
        workGenre: String?,
        existingComposerId: String?,
        openOpusComposerId: String?,
        composerName: String,
        composerEpoch: String?
    ): ApiResult<Work> = safeApiCall {
        val dto = try {
            apiService().findOrCreateWork(
                WorkRequest(
                    title = title,
                    openOpusId = openOpusWorkId,
                    type = workGenre,
                    composers = listOf(
                        ComposerRequest(
                            name = composerName,
                            openOpusId = openOpusComposerId,
                            id = existingComposerId,
                            epoch = composerEpoch
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
        database.withTransaction {
            composersRepository.upsert(dto.composers)
            workDao.upsert(listOf(dto.toRow()))
            workDao.upsertWorkComposers(dto.composers.map { WorkComposerEntity(dto.id, it.id) })
        }
        dto.toDomain()
    }
}
