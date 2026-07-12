package com.chaddy50.concerttracker.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Best-effort trigger to drain the outbox: enqueues a unique, network-constrained [SyncWorker]. The
 * `CONNECTED` constraint defers it until the network is available.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * `KEEP` folds a burst of writes into the single already
     * pending job. Deliberately does **not** disturb a job that is backing off after a failure.
     */
    fun requestSync() = enqueue(ExistingWorkPolicy.KEEP)

    /**
     * Forces a fresh attempt now, resetting any retry backoff. Used on reconnect and for the manual
     * Retry: with plain [requestSync] a job that failed while offline would be sitting in a long
     * exponential backoff, and `KEEP` would leave it there — so regaining connectivity wouldn't
     * actually retry until that (up to multi-hour) backoff elapsed. `REPLACE` drops the stale job and
     * enqueues one eligible to run as soon as the network is up.
     */
    fun requestImmediateSync() = enqueue(ExistingWorkPolicy.REPLACE)

    private fun enqueue(existingWorkPolicy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(15))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, existingWorkPolicy, request)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "sync"
    }
}
