package com.chaddy50.concerttracker.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncWorkerTest {

    private val syncManager: SyncManager = mockk()

    private fun buildWorker(): SyncWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    SyncWorker(appContext, workerParameters, syncManager)
            })
            .build()
    }

    @Test
    fun `returns success on a completed drain and invokes sync once`() = runTest {
        coEvery { syncManager.sync() } returns SyncResult(numberOfOperationsProcessed = 3, didFinish = true)

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { syncManager.sync() }
    }

    @Test
    fun `returns retry when the drain did not complete`() = runTest {
        coEvery { syncManager.sync() } returns SyncResult(numberOfOperationsProcessed = 1, didFinish = false)

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
