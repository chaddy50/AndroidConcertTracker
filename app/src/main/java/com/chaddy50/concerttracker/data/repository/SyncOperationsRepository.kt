package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.domain.SyncJob
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.local.dao.SyncOperationDao
import com.chaddy50.concerttracker.data.local.entity.SyncOperationEntity
import com.chaddy50.concerttracker.data.local.entity.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The public face of the sync outbox (`sync_operations`). Write repositories append ops here as they
 * apply local changes; the [com.chaddy50.concerttracker.data.sync.SyncManager] drains them in FIFO
 * order and records each op's fate. This is the only class that touches [SyncOperationDao].
 */
@Singleton
class SyncOperationsRepository @Inject constructor(
    private val syncOperationDao: SyncOperationDao
) {
    /** Append an op to the outbox, stamped with the current time. Call inside the same Room
     *  transaction as the local write it accompanies so the two are atomic. */
    suspend fun enqueue(
        entityType: SyncEntityType,
        operationType: SyncOperationType,
        entityId: String,
        payloadJson: String?
    ) {
        syncOperationDao.enqueue(
            SyncOperationEntity(
                entityType = entityType.name,
                operationType = operationType.name,
                entityId = entityId,
                payloadJson = payloadJson,
                createdAt = Instant.now().toString()
            )
        )
    }

    /** Drops any queued ops for a never-synced entity being hard-deleted (create-then-delete fold). */
    suspend fun discardForEntity(entityLocalId: String) = syncOperationDao.deleteForEntity(entityLocalId)

    /** Outbox contents in FIFO order, for the drain. */
    suspend fun pending(): List<SyncOperationEntity> = syncOperationDao.getAllOrdered()

    /** Remove a resolved op from the outbox. */
    suspend fun remove(id: Long) = syncOperationDao.delete(id)

    /** Count another attempt at a still-retryable op (e.g. a transient network failure). Leaves the
     *  op PENDING — no `lastError` — so the UI doesn't show a transient hiccup as a hard failure. */
    suspend fun recordRetryableAttempt(id: Long) = syncOperationDao.incrementAttempt(id)

    /** Record a permanently failed attempt, storing the error and flagging the op as failed. */
    suspend fun recordFailure(id: Long, error: String) {
        syncOperationDao.incrementAttempt(id)
        syncOperationDao.markFailed(id, error)
    }

    /** The outbox as a live list of jobs (FIFO), for the sync-status popup. Identifying context is
     *  resolved separately (see SyncJobDescriber) so this repository stays free of read dependencies. */
    fun observeJobs(): Flow<List<SyncJob>> =
        syncOperationDao.observeAll().map { ops -> ops.map { it.toDomain() } }
}
