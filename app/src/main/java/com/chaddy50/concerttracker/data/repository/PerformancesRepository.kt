package com.chaddy50.concerttracker.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.enum.SyncState
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.dao.ComposerDao
import com.chaddy50.concerttracker.data.local.dao.PerformanceDao
import com.chaddy50.concerttracker.data.local.dao.PerformerDao
import com.chaddy50.concerttracker.data.local.dao.SetListEntryDao
import com.chaddy50.concerttracker.data.local.dao.VenueDao
import com.chaddy50.concerttracker.data.local.dao.WorkDao
import com.chaddy50.concerttracker.data.local.entity.HeadlinePerformerEntity
import com.chaddy50.concerttracker.data.external.dataTransferObjects.PerformanceRows
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toPendingRows
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toRows
import com.chaddy50.concerttracker.data.external.dataTransferObjects.withClientIds
import com.chaddy50.concerttracker.data.local.relation.toDomain
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.sync.SyncScheduler
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PerformancesRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val database: ConcertTrackerDatabase,
    private val performanceDao: PerformanceDao,
    private val setListEntryDao: SetListEntryDao,
    private val venueDao: VenueDao,
    private val performerDao: PerformerDao,
    private val workDao: WorkDao,
    private val composerDao: ComposerDao,
    private val syncOperationsRepository: SyncOperationsRepository,
    private val syncScheduler: Provider<SyncScheduler>
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

    // region Reads (Room is the single source of truth)

    fun observeNextUpcomingPerformance(): Flow<Performance?> =
        performanceDao.observeNextUpcoming(Instant.now().toString()).map { it?.toDomain() }

    fun observeRecentlyAttendedPerformances(): Flow<List<Performance>> {
        val now = Instant.now()
        val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS).toString()
        return performanceDao.observeRecentlyAttended(thirtyDaysAgo, now.toString())
            .map { list -> list.map { it.toDomain() } }
    }

    fun observeUpcomingPerformances(): Flow<List<Performance>> =
        performanceDao.observeUpcoming(Instant.now().toString()).map { list -> list.map { it.toDomain() } }

    fun observePastPerformancesPaged(): Flow<PagingData<Performance>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            performanceDao.pagingPast(Instant.now().toString())
        }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    fun observePerformance(id: String): Flow<Performance?> =
        performanceDao.observePerformance(id).map { it?.toDomain() }

    // endregion

    // region Network sync + write-through

    suspend fun loadPerformances(): ApiResult<Unit> = safeApiCall {
        val performances = apiService().getPerformances()
        database.withTransaction {
            performances.forEach { persist(it.toRows()) }
            if (performances.isEmpty()) {
                performanceDao.deleteAllSynced()
            } else {
                performanceDao.deleteSyncedNotIn(performances.map { it.id })
            }
        }
    }

    suspend fun createPerformance(request: PerformanceRequest): ApiResult<Performance> {
        val stamped = request.withClientIds()
        val localId = stamped.id!!
        database.withTransaction {
            persist(stamped.toPendingRows())
            syncOperationsRepository.enqueue(SyncEntityType.PERFORMANCE, SyncOperationType.CREATE, localId, json.encodeToString(stamped))
        }
        syncScheduler.get().requestSync()
        return ApiResult.Success(observePerformance(localId).first()!!)
    }

    suspend fun updatePerformance(id: String, request: PerformanceRequest): ApiResult<Performance> {
        val existing = performanceDao.getById(id) ?: return ApiResult.Error(ApiErrorType.Type.CLIENT)
        database.withTransaction {
            performanceDao.upsert(
                existing.copy(
                    date = request.date,
                    status = request.status.name,
                    venueId = request.venueId,
                    syncState = SyncState.PENDING.toName()
                )
            )
            performanceDao.deleteHeadlinePerformers(id)
            performanceDao.upsertHeadlinePerformers(request.performerIds.map { HeadlinePerformerEntity(id, it) })
            syncOperationsRepository.enqueue(SyncEntityType.PERFORMANCE, SyncOperationType.UPDATE, id, json.encodeToString(request.copy(id = id, notes = existing.notes)))
        }
        syncScheduler.get().requestSync()
        return ApiResult.Success(observePerformance(id).first()!!)
    }

    suspend fun updatePerformanceNotes(id: String, notes: String): ApiResult<Performance> {
        // Snapshot the current graph so the enqueued full-PUT payload doesn't blank out the
        // performance's date/venue/performers/status when it replays (only notes is changing).
        val current = getPerformance(id) ?: return ApiResult.Error(ApiErrorType.Type.CLIENT)
        val snapshot = PerformanceRequest(
            date = current.date,
            venueId = current.venue.id,
            performerIds = current.performers.map { it.id },
            status = current.status,
            notes = notes,
            id = id
        )
        database.withTransaction {
            performanceDao.updateNotes(id, notes, SyncState.PENDING.toName())
            syncOperationsRepository.enqueue(
                SyncEntityType.PERFORMANCE,
                SyncOperationType.UPDATE,
                id,
                json.encodeToString(snapshot)
            )
        }
        syncScheduler.get().requestSync()
        return ApiResult.Success(observePerformance(id).first()!!)
    }

    suspend fun deletePerformance(id: String): ApiResult<Unit> {
        val existing = performanceDao.getById(id) ?: return ApiResult.Success(Unit)
        database.withTransaction {
            if (existing.syncState == SyncState.PENDING.toName()) {
                performanceDao.delete(id)
                syncOperationsRepository.discardForEntity(id)
            } else {
                performanceDao.markSyncState(id, SyncState.PENDING_DELETE.toName())
                syncOperationsRepository.enqueue(SyncEntityType.PERFORMANCE, SyncOperationType.DELETE, id, null)
            }
        }
        syncScheduler.get().requestSync()
        return ApiResult.Success(Unit)
    }

    suspend fun markSynced(id: String) = performanceDao.markSyncState(id, SyncState.SYNCED.toName())

    suspend fun applyServerDeletion(id: String) = performanceDao.delete(id)

    suspend fun markSyncFailed(id: String) = performanceDao.markSyncState(id, SyncState.FAILED.toName())

    /**
     * One-shot Room read of a single performance, used by the edit screens to populate their form
     * working copies. Offline-safe — reads the cache (the single source of truth) rather than the
     * network. Returns null when the performance is not cached.
     */
    suspend fun getPerformance(id: String): Performance? =
        performanceDao.observePerformance(id).first()?.toDomain()

    /** Writes a full performance graph into Room. Caller is responsible for the surrounding transaction. */
    private suspend fun persist(rows: PerformanceRows) {
        venueDao.upsert(rows.venues)
        performerDao.upsert(rows.performers)
        composerDao.upsert(rows.composers)
        workDao.upsert(rows.works)
        workDao.upsertWorkComposers(rows.workComposers)
        performanceDao.upsert(rows.performance)
        performanceDao.deleteHeadlinePerformers(rows.performance.id)
        performanceDao.upsertHeadlinePerformers(rows.headlinePerformers)
        val pendingEntryIds = setListEntryDao.getUnsyncedIdsForPerformance(rows.performance.id).toSet()
        setListEntryDao.deleteSyncedForPerformance(rows.performance.id)
        setListEntryDao.upsert(rows.setListEntries.filter { it.id !in pendingEntryIds })
        setListEntryDao.upsertFeaturedPerformers(rows.featuredPerformers.filter { it.setListEntryId !in pendingEntryIds })
    }

    // endregion
}
