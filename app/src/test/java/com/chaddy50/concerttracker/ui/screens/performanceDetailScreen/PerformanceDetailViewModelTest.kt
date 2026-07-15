package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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

    private fun entry(id: String, notes: String = "") = SetListEntry(
        id = id, work = Work(id = "w-$id", title = "Work $id"), order = 1, notes = notes
    )

    private fun performance(setList: List<SetListEntry> = emptyList(), notes: String = "") = Performance(
        id = "p1",
        date = "2024-06-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"),
        status = PerformanceStatus.ATTENDED,
        setList = setList,
        notes = notes
    )

    private fun viewModel() = PerformanceDetailViewModel(
        SavedStateHandle(mapOf("id" to "p1")),
        performancesRepository,
        setListEntriesRepository
    )

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `uiState emits the observed performance`() = runTest {
        val performance = performance(listOf(entry("e1", "note")))
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PerformanceDetailUiState.Content)
        assertEquals(performance, (state as PerformanceDetailUiState.Content).performance)
    }

    @Test
    fun `uiState is Empty when nothing is cached`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns flowOf(null)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PerformanceDetailUiState.Empty)
    }

    @Test
    fun `uiState re-emits when a set-list entry is added to the cached performance`() = runTest {
        val flow = MutableStateFlow(performance(listOf(entry("e1"))))
        every { performancesRepository.observePerformance("p1") } returns flow
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val initial = viewModel.uiState.value
        assertTrue(initial is PerformanceDetailUiState.Content)
        assertEquals(1, (initial as PerformanceDetailUiState.Content).performance.setList.size)

        flow.value = performance(listOf(entry("e1"), entry("e2")))
        advanceUntilIdle()

        val updated = viewModel.uiState.value
        assertTrue(updated is PerformanceDetailUiState.Content)
        assertEquals(2, (updated as PerformanceDetailUiState.Content).performance.setList.size)
    }

    @Test
    fun `autoSaveNotes sets didSavingNotesHaveError when save fails`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns
            flowOf(performance(listOf(entry("e1", "old"))))
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
        every { performancesRepository.observePerformance("p1") } returns
            flowOf(performance(listOf(entry("e1", "a"), entry("e2", "b"))))
        coEvery { setListEntriesRepository.updateSetListEntry(any(), any()) } returns
            ApiResult.Success(entry("e1", "x"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftNote("e1", "x")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertNull(viewModel.didSavingNotesHaveError)
        coVerify(exactly = 1) {
            setListEntriesRepository.updateSetListEntry("e1", "x")
        }
        coVerify(exactly = 0) { setListEntriesRepository.updateSetListEntry("e2", any()) }
    }

    @Test
    fun `seeds the performance-note draft from the cached performance`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance(notes = "seed"))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("seed", viewModel.draftPerformanceNotes)
    }

    @Test
    fun `seeds an empty performance-note draft when notes are empty`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance(notes = ""))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("", viewModel.draftPerformanceNotes)
    }

    @Test
    fun `a debounced performance-note edit saves once, then clears`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance(notes = "seed"))
        coEvery { performancesRepository.updatePerformanceNotes(any(), any()) } returns
            ApiResult.Success(performance(notes = "typed"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftPerformanceNotes("typed")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        coVerify(exactly = 1) { performancesRepository.updatePerformanceNotes("p1", "typed") }
        assertNull(viewModel.didSavingNotesHaveError)

        // Clearing saves an explicit empty string.
        viewModel.updateDraftPerformanceNotes("")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        coVerify(exactly = 1) { performancesRepository.updatePerformanceNotes("p1", "") }
    }

    @Test
    fun `does not save the performance note when the draft equals the cached value`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance(notes = "seed"))
        coEvery { performancesRepository.updatePerformanceNotes(any(), any()) } returns
            ApiResult.Success(performance(notes = "seed"))
        val viewModel = viewModel()
        advanceUntilIdle()

        // Re-set the draft to the same value the seed already holds.
        viewModel.updateDraftPerformanceNotes("seed")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        coVerify(exactly = 0) { performancesRepository.updatePerformanceNotes(any(), any()) }
    }

    @Test
    fun `does not clobber an in-progress performance-note edit when Room re-emits`() = runTest {
        val flow = MutableStateFlow(performance(notes = "server"))
        every { performancesRepository.observePerformance("p1") } returns flow
        coEvery { performancesRepository.updatePerformanceNotes(any(), any()) } returns
            ApiResult.Success(performance(notes = "server"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftPerformanceNotes("in progress")
        flow.value = performance(notes = "server") // a write-through re-emission of the old value
        advanceUntilIdle()

        assertEquals("in progress", viewModel.draftPerformanceNotes)
    }

    @Test
    fun `performance-note save failure sets and later clears didSavingNotesHaveError`() = runTest {
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance(notes = "seed"))
        coEvery { performancesRepository.updatePerformanceNotes("p1", "boom") } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        coEvery { performancesRepository.updatePerformanceNotes("p1", "ok") } returns
            ApiResult.Success(performance(notes = "ok"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateDraftPerformanceNotes("boom")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
        assertEquals(ApiErrorType.Type.SERVER, viewModel.didSavingNotesHaveError)

        viewModel.updateDraftPerformanceNotes("ok")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
        assertNull(viewModel.didSavingNotesHaveError)
    }
}
