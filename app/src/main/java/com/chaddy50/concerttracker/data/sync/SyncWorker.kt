package com.chaddy50.concerttracker.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Drains the outbox in the background under a CONNECTED constraint. A completed drain is a success;
 * a drain that stopped on a transient error asks WorkManager to retry with backoff.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result = syncManager.sync()
        return if (result.didFinish) Result.success() else Result.retry()
    }
}
