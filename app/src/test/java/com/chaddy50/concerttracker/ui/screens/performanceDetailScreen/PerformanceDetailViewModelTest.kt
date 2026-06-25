package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.SetListEntryRequest
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformanceDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val performancesRepository: PerformancesRepository = mockk()
    private val setListEntriesRepository: SetListEntriesRepository = mockk()

    private fun entry(id: String, notes: String?) = SetListEntry(
        id = id, work = Work(id = "w-$id", title = "Work $id"), order = 1, notes = notes
    )

    private fun performance(setList: List<SetListEntry>) = Performance(
        id = "p1",
        date = "2024-06-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"),
        status = PerformanceStatus.ATTENDED,
        setList = setList
    )

    private fun viewModel() = PerformanceDetailViewModel(
        SavedStateHandle(mapOf("id" to "p1")),
        performancesRepository,
        setListEntriesRepository
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPerformance transitions Loading then Success`() = runTest {
        val performance = performance(listOf(entry("e1", "note")))
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance)
        val viewModel = viewModel()
        assertTrue(viewModel.uiState is PerformanceDetailUiState.Loading)
        advanceUntilIdle()
        assertEquals(PerformanceDetailUiState.Success(performance), viewModel.uiState)
    }

    @Test
    fun `loadPerformance transitions to Error with errorType on failure`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(PerformanceDetailUiState.Error(ApiErrorType.Type.NETWORK), viewModel.uiState)
    }

    @Test
    fun `autoSaveNotes sets didSavingNotesHaveError when save fails`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            ApiResult.Success(performance(listOf(entry("e1", "old"))))
        coEvery { setListEntriesRepository.updateSetListEntry(any(), any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftNote("e1", "new")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(ApiErrorType.Type.SERVER, viewModel.didSavingNotesHaveError)
    }

    @Test
    fun `autoSaveNotes clears error and only sends changed notes`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            ApiResult.Success(performance(listOf(entry("e1", "a"), entry("e2", "b"))))
        coEvery { setListEntriesRepository.updateSetListEntry(any(), any()) } returns
            ApiResult.Success(entry("e1", "x"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftNote("e1", "x")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertNull(viewModel.didSavingNotesHaveError)
        coVerify(exactly = 1) {
            setListEntriesRepository.updateSetListEntry("e1", SetListEntryRequest(notes = "x"))
        }
        coVerify(exactly = 0) { setListEntriesRepository.updateSetListEntry("e2", any()) }
    }
}
