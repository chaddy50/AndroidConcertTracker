package com.chaddy50.concerttracker.util

import com.chaddy50.concerttracker.data.domain.SyncJob
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.data.repository.WorksRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enriches a [SyncJob] with the identifying context (name + associated performance date) of the
 * entity it targets, read through the domain repositories. Best-effort: a missing lookup just leaves
 * the field null. The row is still present at describe time — a queued op always has its cached
 * entity (a delete is a tombstone, not a hard delete, until the server confirms).
 */
@Singleton
class SyncJobDescriber @Inject constructor(
    private val performancesRepository: PerformancesRepository,
    private val setListEntriesRepository: SetListEntriesRepository,
    private val performersRepository: PerformersRepository,
    private val worksRepository: WorksRepository
) {
    suspend fun describe(job: SyncJob): SyncJob = when (job.entityType) {
        SyncEntityType.PERFORMANCE -> {
            val performance = performancesRepository.observePerformance(job.entityId).first()
            job.copy(description = performance?.venue?.name, performanceDateIso = performance?.date)
        }
        SyncEntityType.SET_LIST_ENTRY -> {
            val performanceId = setListEntriesRepository.getParentPerformanceId(job.entityId)
            val performance = performanceId?.let { performancesRepository.observePerformance(it).first() }
            val entry = performance?.setList?.find { it.id == job.entityId }
            job.copy(description = entry?.work?.title, performanceDateIso = performance?.date)
        }
        SyncEntityType.PERFORMER -> job.copy(description = performersRepository.getPerformer(job.entityId)?.name)
        SyncEntityType.WORK -> job.copy(description = worksRepository.getWork(job.entityId)?.title)
    }
}
