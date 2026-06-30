package com.chaddy50.concerttracker.data.repository

import androidx.room.withTransaction
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
import com.chaddy50.concerttracker.data.external.dataTransferObjects.PerformanceRows
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toDomain
import com.chaddy50.concerttracker.data.external.dataTransferObjects.toRows
import com.chaddy50.concerttracker.data.local.relation.toDomain
import com.chaddy50.concerttracker.data.domain.Performance
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
    private val composerDao: ComposerDao
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
        performanceDao.observeNextUpcoming().map { it?.toDomain() }

    fun observeRecentlyAttendedPerformances(): Flow<List<Performance>> {
        val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).toString()
        return performanceDao.observeRecentlyAttended(thirtyDaysAgo).map { list -> list.map { it.toDomain() } }
    }

    fun observeUpcomingPerformances(): Flow<List<Performance>> =
        performanceDao.observeUpcoming().map { list -> list.map { it.toDomain() } }

    fun observePastPerformances(): Flow<List<Performance>> =
        performanceDao.observePast().map { list -> list.map { it.toDomain() } }

    fun observePerformance(id: String): Flow<Performance?> =
        performanceDao.observePerformance(id).map { it?.toDomain() }

    // endregion

    // region Network sync + write-through

    suspend fun loadPerformances(): ApiResult<Unit> = safeApiCall {
        val performances = apiService().getPerformances()
        database.withTransaction {
            performances.forEach { persist(it.toRows()) }
            if (performances.isEmpty()) {
                performanceDao.deleteAll()
            } else {
                performanceDao.deleteNotIn(performances.map { it.id })
            }
        }
    }

    suspend fun createPerformance(request: PerformanceRequest): ApiResult<Performance> = safeApiCall {
        val created = apiService().createPerformance(request)
        database.withTransaction { persist(created.toRows()) }
        created.toDomain()
    }

    suspend fun updatePerformance(id: String, request: PerformanceRequest): ApiResult<Performance> = safeApiCall {
        val updated = apiService().updatePerformance(id, request)
        database.withTransaction { persist(updated.toRows()) }
        updated.toDomain()
    }

    suspend fun deletePerformance(id: String): ApiResult<Unit> = safeApiCall {
        apiService().deletePerformance(id)
        performanceDao.delete(id)
    }

    /**
     * One-shot network fetch of a single performance, written through to the cache. Used by the
     * edit screens to populate their form working copies.
     */
    suspend fun getPerformance(id: String): ApiResult<Performance> = safeApiCall {
        val performance = apiService().getPerformance(id)
        database.withTransaction { persist(performance.toRows()) }
        performance.toDomain()
    }

    /**
     * Re-fetches a single performance into the cache so observers re-emit. Used by
     * [SetListEntriesRepository] after a set-list mutation.
     */
    suspend fun loadPerformance(id: String): ApiResult<Unit> = when (val result = getPerformance(id)) {
        is ApiResult.Success -> ApiResult.Success(Unit)
        is ApiResult.Error -> result
    }

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
        setListEntryDao.deleteForPerformance(rows.performance.id)
        setListEntryDao.upsert(rows.setListEntries)
        setListEntryDao.upsertFeaturedPerformers(rows.featuredPerformers)
    }

    // endregion
}
