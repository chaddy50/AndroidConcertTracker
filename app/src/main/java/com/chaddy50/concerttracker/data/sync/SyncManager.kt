package com.chaddy50.concerttracker.data.sync

import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
import com.chaddy50.concerttracker.data.external.api.PerformerRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryUpdateRequest
import com.chaddy50.concerttracker.data.external.api.WorkRequest
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.chaddy50.concerttracker.data.local.entity.SyncOperationEntity
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import com.chaddy50.concerttracker.data.repository.SyncOperationsRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val syncOperationsRepository: SyncOperationsRepository,
    private val performancesRepository: PerformancesRepository,
    private val setListEntriesRepository: SetListEntriesRepository
) {
    private val mutex = Mutex()

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

    suspend fun sync(): SyncResult = mutex.withLock {
        val api = apiService()
        var processed = 0
        for (op in syncOperationsRepository.pending()) {
            val opType = SyncOperationType.valueOf(op.operationType)
            when (val result = safeApiCall { performCall(api, op) }) {
                is ApiResult.Success -> {
                    resolveSuccess(op, opType)
                    syncOperationsRepository.remove(op.id)
                    processed++
                }
                is ApiResult.Error -> when {
                    // CREATE 409 = already on the server under our id; UPDATE 409 = last-write-wins
                    // (our local write stands). Either way the op is done.
                    result.errorType == ApiErrorType.Type.CONFLICT -> {
                        resolveSuccess(op, opType)
                        syncOperationsRepository.remove(op.id)
                        processed++
                    }
                    // A DELETE that 404s is already gone — idempotent success.
                    opType == SyncOperationType.DELETE && result.errorType == ApiErrorType.Type.CLIENT -> {
                        resolveSuccess(op, opType)
                        syncOperationsRepository.remove(op.id)
                        processed++
                    }
                    // Transient (offline/timeout/5xx). Normally stop and let the worker retry the
                    // whole outbox later, leaving it FIFO-intact and the op still PENDING (not a
                    // user-facing failure). But once an op has burned through its attempts, give up on
                    // it — flag it FAILED and carry on — so one poison op can't wedge the queue forever.
                    result.errorType.isTransient() -> {
                        if (op.attemptCount + 1 >= MAX_ATTEMPTS) {
                            syncOperationsRepository.recordFailure(op.id, result.errorType.name)
                            markRowFailed(op)
                        } else {
                            syncOperationsRepository.recordRetryableAttempt(op.id)
                            return@withLock SyncResult(processed, didFinish = false)
                        }
                    }
                    // Permanent (client) error — flag it and move on.
                    else -> {
                        syncOperationsRepository.recordFailure(op.id, result.errorType.name)
                        markRowFailed(op)
                    }
                }
            }
        }
        // The outbox drained (some ops may remain FAILED). Pull the server's latest into Room.
        performancesRepository.loadPerformances()
        SyncResult(processed, didFinish = true)
    }

    /** Remove a single (typically failed) op from the outbox, first reverting its local entity
     *  row so no orphan is left behind. No network — local reconciliation only. */
    suspend fun discard(opId: Long) = mutex.withLock {
        val op = syncOperationsRepository.get(opId) ?: return@withLock
        reconcileDiscard(op)
        syncOperationsRepository.remove(op.id)
    }

    private suspend fun reconcileDiscard(op: SyncOperationEntity) {
        val entityType = SyncEntityType.valueOf(op.entityType)
        when (SyncOperationType.valueOf(op.operationType)) {
            // A never-synced create: hard-delete the local row (the change is abandoned).
            // Custom performer/work rows have no syncState — leave them, just drop the op.
            SyncOperationType.CREATE -> when (entityType) {
                SyncEntityType.PERFORMANCE -> performancesRepository.applyServerDeletion(op.entityId)
                SyncEntityType.SET_LIST_ENTRY -> setListEntriesRepository.applyServerDeletion(op.entityId)
                SyncEntityType.PERFORMER, SyncEntityType.WORK -> {}
            }
            // Keep the local edit but stop trying to push it: mark the row SYNCED.
            SyncOperationType.UPDATE -> when (entityType) {
                SyncEntityType.PERFORMANCE -> performancesRepository.markSynced(op.entityId)
                SyncEntityType.SET_LIST_ENTRY -> setListEntriesRepository.markSynced(op.entityId)
                else -> {}
            }
            // Restore the tombstoned row (PENDING_DELETE -> SYNCED).
            SyncOperationType.DELETE -> when (entityType) {
                SyncEntityType.PERFORMANCE -> performancesRepository.markSynced(op.entityId)
                SyncEntityType.SET_LIST_ENTRY -> setListEntriesRepository.markSynced(op.entityId)
                else -> {}
            }
        }
    }

    private suspend fun performCall(api: ConcertTrackerApiService, op: SyncOperationEntity) {
        val entityType = SyncEntityType.valueOf(op.entityType)
        when (SyncOperationType.valueOf(op.operationType)) {
            SyncOperationType.CREATE -> when (entityType) {
                SyncEntityType.PERFORMANCE -> api.createPerformance(decode<PerformanceRequest>(op))
                SyncEntityType.SET_LIST_ENTRY -> api.createSetListEntry(decode<SetListEntryCreateRequest>(op))
                SyncEntityType.PERFORMER -> api.findOrCreatePerformer(decode<PerformerRequest>(op))
                SyncEntityType.WORK -> api.findOrCreateWork(decode<WorkRequest>(op))
            }
            SyncOperationType.UPDATE -> when (entityType) {
                SyncEntityType.PERFORMANCE -> api.updatePerformance(op.entityId, decode<PerformanceRequest>(op))
                SyncEntityType.SET_LIST_ENTRY -> api.updateSetListEntry(op.entityId, decode<SetListEntryUpdateRequest>(op))
                SyncEntityType.PERFORMER -> api.updatePerformer(op.entityId, decode<PerformerRequest>(op))
                else -> error("no UPDATE op for $entityType")
            }
            SyncOperationType.DELETE -> when (entityType) {
                SyncEntityType.PERFORMANCE -> api.deletePerformance(op.entityId)
                SyncEntityType.SET_LIST_ENTRY -> api.deleteSetListEntry(op.entityId)
                else -> error("no DELETE op for $entityType")
            }
        }
    }

    /** Mark the local row SYNCED (create/update) or hard-delete it (delete). Custom performer/work
     *  rows have no syncState column, so their success is just the op being removed. */
    private suspend fun resolveSuccess(op: SyncOperationEntity, opType: SyncOperationType) {
        val entityType = SyncEntityType.valueOf(op.entityType)
        if (opType == SyncOperationType.DELETE) {
            when (entityType) {
                SyncEntityType.PERFORMANCE -> performancesRepository.applyServerDeletion(op.entityId)
                SyncEntityType.SET_LIST_ENTRY -> setListEntriesRepository.applyServerDeletion(op.entityId)
                else -> {}
            }
        } else {
            when (entityType) {
                SyncEntityType.PERFORMANCE -> performancesRepository.markSynced(op.entityId)
                SyncEntityType.SET_LIST_ENTRY -> setListEntriesRepository.markSynced(op.entityId)
                else -> {}
            }
        }
    }

    private suspend fun markRowFailed(op: SyncOperationEntity) {
        when (SyncEntityType.valueOf(op.entityType)) {
            SyncEntityType.PERFORMANCE -> performancesRepository.markSyncFailed(op.entityId)
            SyncEntityType.SET_LIST_ENTRY -> setListEntriesRepository.markSyncFailed(op.entityId)
            else -> {}
        }
    }

    private inline fun <reified T> decode(op: SyncOperationEntity): T =
        json.decodeFromString(requireNotNull(op.payloadJson) { "missing payload for op ${op.id}" })

    private fun ApiErrorType.Type.isTransient(): Boolean =
        this == ApiErrorType.Type.NETWORK || this == ApiErrorType.Type.TIMEOUT ||
            this == ApiErrorType.Type.SERVER || this == ApiErrorType.Type.UNKNOWN

    companion object {
        /** How many times an op may fail transiently before we give up on it and move past it. */
        const val MAX_ATTEMPTS = 5
    }
}
