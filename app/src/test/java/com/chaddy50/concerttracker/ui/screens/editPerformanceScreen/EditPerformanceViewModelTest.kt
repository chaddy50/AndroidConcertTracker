package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditPerformanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val performancesRepository: PerformancesRepository = mockk()
    private val performersRepository: PerformersRepository = mockk()

    private fun performer(id: String, mbId: String?) =
        Performer(id = id, name = "Performer $id", type = PerformerType.SOLO, musicbrainzId = mbId)

    private fun entry(id: String, order: Int) =
        SetListEntry(id = id, work = Work(id = "w-$id", title = "Work $id"), order = order)

    private fun performance(setList: List<SetListEntry> = emptyList(), performers: List<Performer> = emptyList()) =
        Performance(
            id = "p1",
            date = "2024-06-01T19:00:00Z",
            venue = Venue("v1", "Hall", "123", "way"),
            performers = performers,
            status = PerformanceStatus.UPCOMING,
            setList = setList
        )

    private fun editViewModel() = EditPerformanceViewModel(
        SavedStateHandle(mapOf("id" to "p1")), performancesRepository, performersRepository
    )

    private fun createViewModel() = EditPerformanceViewModel(
        SavedStateHandle(mapOf("id" to null)), performancesRepository, performersRepository
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
    fun `loadPerformance populates draft fields on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            ApiResult.Success(performance())
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance())
        val viewModel = editViewModel()
        advanceUntilIdle()

        assertEquals(PerformanceEditUiState.Ready, viewModel.uiState)
        assertNotNull(viewModel.draftDate)
        assertEquals("v1", viewModel.draftVenueId)
        assertEquals(PerformanceStatus.UPCOMING, viewModel.draftStatus)
    }

    @Test
    fun `loadPerformance shows Error with errorType on failure`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            ApiResult.Error(ApiErrorType.Type.TIMEOUT)
        every { performancesRepository.observePerformance("p1") } returns flowOf(null)
        val viewModel = editViewModel()
        advanceUntilIdle()
        assertEquals(PerformanceEditUiState.Error(ApiErrorType.Type.TIMEOUT), viewModel.uiState)
    }

    @Test
    fun `addDraftPerformer adds on Success and skips duplicates`() = runTest {
        coEvery { performersRepository.createPerformer(any()) } returns
            ApiResult.Success(performer("perf1", mbId = "mb1"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addDraftPerformer("mb1", "Performer perf1", "SOLO", null)
        advanceUntilIdle()
        assertEquals(listOf("perf1"), viewModel.draftPerformers.map { it.id })

        viewModel.addDraftPerformer("mb1", "Performer perf1", "SOLO", null)
        advanceUntilIdle()
        assertEquals(1, viewModel.draftPerformers.size)
        coVerify(exactly = 1) { performersRepository.createPerformer(any()) }
    }

    @Test
    fun `addDraftPerformer sets saveError on Error`() = runTest {
        coEvery { performersRepository.createPerformer(any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addDraftPerformer("mb1", "Name", "SOLO", null)
        advanceUntilIdle()
        assertTrue(viewModel.draftPerformers.isEmpty())
        assertNotNull(viewModel.saveError)
    }

    @Test
    fun `currentSetList observes Room, sorts by order, and updates on write-through`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        val roomFlow = MutableStateFlow(performance(setList = listOf(entry("e2", 2), entry("e1", 1))))
        every { performancesRepository.observePerformance("p1") } returns roomFlow
        val viewModel = editViewModel()
        advanceUntilIdle()
        assertEquals(listOf("e1", "e2"), viewModel.currentSetList.map { it.id })

        // a set-list mutation writes through to Room; the observed flow re-emits with no manual refresh
        roomFlow.value = performance(setList = listOf(entry("e1", 1), entry("e2", 2), entry("e3", 3)))
        advanceUntilIdle()
        assertEquals(listOf("e1", "e2", "e3"), viewModel.currentSetList.map { it.id })
    }

    @Test
    fun `savePerformance invokes onSaved on Success and toggles isSaving`() = runTest {
        coEvery { performancesRepository.createPerformance(any()) } returns
            ApiResult.Success(performance())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.updateDraftDate(1_700_000_000_000L)
        viewModel.updateDraftVenue("v1", "Hall")

        var saved = false
        viewModel.savePerformance { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertFalse(viewModel.isSaving)
        coVerify(exactly = 1) { performancesRepository.createPerformance(any()) }
    }

    @Test
    fun `savePerformance sets saveError on Error`() = runTest {
        coEvery { performancesRepository.createPerformance(any()) } returns
            ApiResult.Error(ApiErrorType.Type.CONFLICT)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.updateDraftDate(1_700_000_000_000L)
        viewModel.updateDraftVenue("v1", "Hall")

        var saved = false
        viewModel.savePerformance { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertFalse(viewModel.isSaving)
        assertNotNull(viewModel.saveError)
    }

    @Test
    fun `deletePerformance invokes onDeleted on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance())
        coEvery { performancesRepository.deletePerformance("p1") } returns ApiResult.Success(Unit)
        val viewModel = editViewModel()
        advanceUntilIdle()

        var deleted = false
        viewModel.deletePerformance { deleted = true }
        advanceUntilIdle()
        assertTrue(deleted)
    }

    @Test
    fun `deletePerformance sets saveError on Error`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance())
        coEvery { performancesRepository.deletePerformance("p1") } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = editViewModel()
        advanceUntilIdle()

        var deleted = false
        viewModel.deletePerformance { deleted = true }
        advanceUntilIdle()
        assertFalse(deleted)
        assertNotNull(viewModel.saveError)
    }
}
