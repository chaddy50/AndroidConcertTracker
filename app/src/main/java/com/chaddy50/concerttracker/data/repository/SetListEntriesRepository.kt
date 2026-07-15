package com.chaddy50.concerttracker.data.repository

import androidx.room.withTransaction
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.enum.SyncState
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.FeaturedPerformerRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryUpdateRequest
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.dao.SetListEntryDao
import com.chaddy50.concerttracker.data.local.entity.FeaturedPerformerEntity
import com.chaddy50.concerttracker.data.local.entity.SetListEntryEntity
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.sync.SyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class SetListEntriesRepository @Inject constructor(
    private val json: Json,
    private val database: ConcertTrackerDatabase,
    private val setListEntryDao: SetListEntryDao,
    private val syncOperationsRepository: SyncOperationsRepository,
    private val performancesRepository: PerformancesRepository,
    private val syncScheduler: Provider<SyncScheduler>
) {

    suspend fun createSetListEntry(request: SetListEntryCreateRequest): ApiResult<SetListEntry> {
        val localId = request.id ?: UUID.randomUUID().toString()
        database.withTransaction {
            setListEntryDao.upsert(
                listOf(
                    SetListEntryEntity(
                        id = localId,
                        performanceId = request.performanceId,
                        workId = request.workId,
                        order = request.order,
                        syncState = SyncState.PENDING.toName()
                    )
                )
            )
            setListEntryDao.upsertFeaturedPerformers(
                request.featuredPerformers.map { FeaturedPerformerEntity(localId, it.performerId, it.role) }
            )
            enqueue(SyncOperationType.CREATE, localId, json.encodeToString(request.copy(id = localId)))
        }
        syncScheduler.get().requestSync()
        return entryResult(request.performanceId, localId)
    }

    suspend fun updateSetListEntry(id: String, notes: String): ApiResult<SetListEntry> {
        val existing = setListEntryDao.getById(id) ?: return ApiResult.Error(ApiErrorType.Type.CLIENT)
        database.withTransaction {
            setListEntryDao.updateNotes(id, notes, SyncState.PENDING.toName())
            enqueue(SyncOperationType.UPDATE, id, json.encodeToString(snapshotRequest(id, notes = notes)))
        }
        syncScheduler.get().requestSync()
        return entryResult(existing.performanceId, id)
    }

    suspend fun updateSetListEntryFull(id: String, request: SetListEntryUpdateRequest): ApiResult<SetListEntry> {
        val existing = setListEntryDao.getById(id) ?: return ApiResult.Error(ApiErrorType.Type.CLIENT)
        database.withTransaction {
            setListEntryDao.upsert(
                listOf(
                    existing.copy(
                        workId = request.workId ?: existing.workId,
                        order = request.order ?: existing.order,
                        syncState = SyncState.PENDING.toName()
                    )
                )
            )
            request.featuredPerformers?.let { featured ->
                setListEntryDao.deleteFeaturedPerformers(id)
                setListEntryDao.upsertFeaturedPerformers(
                    featured.map { FeaturedPerformerEntity(id, it.performerId, it.role) }
                )
            }
            // A full edit doesn't change notes, so snapshot preserves the existing value.
            enqueue(SyncOperationType.UPDATE, id, json.encodeToString(snapshotRequest(id, notes = existing.notes)))
        }
        syncScheduler.get().requestSync()
        return entryResult(existing.performanceId, id)
    }

    private suspend fun snapshotRequest(id: String, notes: String): SetListEntryUpdateRequest {
        val entry = requireNotNull(setListEntryDao.getById(id))
        val featured = setListEntryDao.getFeaturedPerformers(id)
            .map { FeaturedPerformerRequest(it.performerId, it.role) }
        return SetListEntryUpdateRequest(
            workId = entry.workId,
            order = entry.order,
            featuredPerformers = featured,
            notes = notes
        )
    }

    suspend fun deleteSetListEntry(id: String): ApiResult<Unit> {
        val existing = setListEntryDao.getById(id) ?: return ApiResult.Success(Unit)
        database.withTransaction {
            if (existing.syncState == SyncState.PENDING.toName()) {
                setListEntryDao.delete(id)
                syncOperationsRepository.discardForEntity(id)
            } else {
                setListEntryDao.markSyncState(id, SyncState.PENDING_DELETE.toName())
                enqueue(SyncOperationType.DELETE, id, null)
            }
        }
        syncScheduler.get().requestSync()
        return ApiResult.Success(Unit)
    }

    suspend fun getParentPerformanceId(entryId: String): String? = setListEntryDao.getPerformanceIdFor(entryId)

    suspend fun markSynced(id: String) = setListEntryDao.markSyncState(id, SyncState.SYNCED.toName())

    suspend fun applyServerDeletion(id: String) = setListEntryDao.delete(id)

    suspend fun markSyncFailed(id: String) = setListEntryDao.markSyncState(id, SyncState.FAILED.toName())

    private suspend fun entryResult(performanceId: String, entryId: String): ApiResult<SetListEntry> {
        val entry = performancesRepository.observePerformance(performanceId).first()
            ?.setList?.find { it.id == entryId }
        return entry?.let { ApiResult.Success(it) } ?: ApiResult.Error(ApiErrorType.Type.UNKNOWN)
    }

    private suspend fun enqueue(operationType: SyncOperationType, entityLocalId: String, payloadJson: String?) =
        syncOperationsRepository.enqueue(SyncEntityType.SET_LIST_ENTRY, operationType, entityLocalId, payloadJson)
}
