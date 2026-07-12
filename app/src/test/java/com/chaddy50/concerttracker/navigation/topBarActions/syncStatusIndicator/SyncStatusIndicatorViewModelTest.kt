package com.chaddy50.concerttracker.navigation.topBarActions.syncStatusIndicator

import com.chaddy50.concerttracker.data.domain.SyncJob
import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.repository.SyncOperationsRepository
import com.chaddy50.concerttracker.data.sync.SyncScheduler
import com.chaddy50.concerttracker.util.SyncJobDescriber
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusIndicatorViewModelTest {

    private val repository: SyncOperationsRepository = mockk()
    private val syncJobDescriber: SyncJobDescriber = mockk()
    private val syncScheduler: SyncScheduler = mockk(relaxed = true)

    private fun job(id: Long, failed: Boolean = false) = SyncJob(
        id = id,
        entityId = "e$id",
        entityType = SyncEntityType.PERFORMANCE,
        operationType = SyncOperationType.CREATE,
        failed = failed
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        // The describer is exercised in its own test; here it passes jobs through untouched.
        coEvery { syncJobDescriber.describe(any()) } answers { firstArg() }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `uiState reflects the queued jobs and updates reactively`() = runTest {
        val jobs = MutableStateFlow(emptyList<SyncJob>())
        every { repository.observeJobs() } returns jobs
        val viewModel = SyncStatusIndicatorViewModel(repository, syncJobDescriber, syncScheduler)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasWork)

        jobs.value = listOf(job(1), job(2), job(3))
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.queuedCount)
        assertTrue(viewModel.uiState.value.hasWork)
    }

    @Test
    fun `a failed job sets the failure flag`() = runTest {
        every { repository.observeJobs() } returns flowOf(listOf(job(1), job(2, failed = true)))
        val viewModel = SyncStatusIndicatorViewModel(repository, syncJobDescriber, syncScheduler)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasFailure)
    }

    @Test
    fun `retry re-triggers a sync`() = runTest {
        every { repository.observeJobs() } returns flowOf(emptyList())
        val viewModel = SyncStatusIndicatorViewModel(repository, syncJobDescriber, syncScheduler)

        viewModel.retry()

        verify { syncScheduler.requestImmediateSync() }
    }
}
