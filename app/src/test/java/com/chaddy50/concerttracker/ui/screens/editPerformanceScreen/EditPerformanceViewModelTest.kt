package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
class EditPerformanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val performancesRepository: PerformancesRepository = mockk()
    private val setListEntriesRepository: SetListEntriesRepository = mockk()

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
        SavedStateHandle(mapOf("id" to "p1")), performancesRepository, setListEntriesRepository
    )

    private fun createViewModel() = EditPerformanceViewModel(
        SavedStateHandle(mapOf("id" to null)), performancesRepository, setListEntriesRepository
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
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
        every { performancesRepository.observePerformance("p1") } returns flowOf(performance())
        val viewModel = editViewModel()
        advanceUntilIdle()

        assertEquals(PerformanceEditUiState.Ready, viewModel.uiState)
        assertNotNull(viewModel.draftDate)
        assertEquals("v1", viewModel.draftVenueId)
        assertEquals(PerformanceStatus.UPCOMING, viewModel.draftStatus)
    }

    @Test
    fun `loadPerformance shows NotFound when the performance is not cached`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns null
        every { performancesRepository.observePerformance("p1") } returns flowOf(null)
        val viewModel = editViewModel()
        advanceUntilIdle()
        assertEquals(PerformanceEditUiState.NotFound, viewModel.uiState)
    }

    @Test
    fun `addDraftPerformer adds the already-materialized performer and skips duplicates by our id`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addDraftPerformer("perf1", "Performer perf1", "SOLO", null)
        assertEquals(listOf("perf1"), viewModel.draftPerformers.map { it.id })
        assertEquals(PerformerType.SOLO, viewModel.draftPerformers.single().type)

        viewModel.addDraftPerformer("perf1", "Performer perf1", "SOLO", null)
        assertEquals(1, viewModel.draftPerformers.size)
    }

    @Test
    fun `addDraftPerformer performs no network create`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addDraftPerformer("perf1", "Name", "SOLO", null)
        assertEquals(1, viewModel.draftPerformers.size)
        assertNull(viewModel.saveError)
    }

    @Test
    fun `currentSetList observes Room, sorts by order, and updates on write-through`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
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
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
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
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
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

    @Test
    fun `moveSetListEntry reorders the draft locally without persisting until save`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
        every { performancesRepository.observePerformance("p1") } returns
            flowOf(performance(setList = listOf(entry("e1", 1), entry("e2", 2), entry("e3", 3))))
        val viewModel = editViewModel()
        advanceUntilIdle()

        viewModel.moveSetListEntry(from = 0, to = 2)
        advanceUntilIdle()

        assertEquals(listOf("e2", "e3", "e1"), viewModel.currentSetList.map { it.id })
        // Cancelling (navigating away) must undo the reorder, so nothing is persisted on the drag itself.
        coVerify(exactly = 0) { setListEntriesRepository.reorderSetListEntries(any()) }
    }

    @Test
    fun `savePerformance commits the drafted set list order`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            performance(setList = listOf(entry("e1", 1), entry("e2", 2), entry("e3", 3)))
        every { performancesRepository.observePerformance("p1") } returns
            flowOf(performance(setList = listOf(entry("e1", 1), entry("e2", 2), entry("e3", 3))))
        coEvery { performancesRepository.updatePerformance(any(), any()) } returns
            ApiResult.Success(performance())
        coEvery { setListEntriesRepository.reorderSetListEntries(any()) } returns ApiResult.Success(Unit)
        val viewModel = editViewModel()
        advanceUntilIdle()

        viewModel.moveSetListEntry(from = 0, to = 2)
        viewModel.savePerformance {}
        advanceUntilIdle()

        coVerify(exactly = 1) { setListEntriesRepository.reorderSetListEntries(listOf("e2", "e3", "e1")) }
    }

    @Test
    fun `savePerformance does not touch the set list order when nothing was reordered`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            performance(setList = listOf(entry("e1", 1), entry("e2", 2)))
        every { performancesRepository.observePerformance("p1") } returns
            flowOf(performance(setList = listOf(entry("e1", 1), entry("e2", 2))))
        coEvery { performancesRepository.updatePerformance(any(), any()) } returns
            ApiResult.Success(performance())
        val viewModel = editViewModel()
        advanceUntilIdle()

        viewModel.savePerformance {}
        advanceUntilIdle()

        coVerify(exactly = 0) { setListEntriesRepository.reorderSetListEntries(any()) }
    }

    @Test
    fun `moveSetListEntry to the same index is a no-op`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
        every { performancesRepository.observePerformance("p1") } returns
            flowOf(performance(setList = listOf(entry("e1", 1), entry("e2", 2))))
        val viewModel = editViewModel()
        advanceUntilIdle()

        viewModel.moveSetListEntry(from = 1, to = 1)
        advanceUntilIdle()

        assertEquals(listOf("e1", "e2"), viewModel.currentSetList.map { it.id })
    }

    @Test
    fun `addPendingSetListEntry assigns 1-based order from list size`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addPendingSetListEntry("w1", "First", "Composer", emptyList())
        viewModel.addPendingSetListEntry("w2", "Second", "Composer", emptyList())

        assertEquals(listOf(1, 2), viewModel.pendingSetListEntries.map { it.order })
    }

    @Test
    fun `movePendingSetListEntry reorders in memory and reassigns 1-based orders without the repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.addPendingSetListEntry("w1", "First", "Composer", emptyList())
        viewModel.addPendingSetListEntry("w2", "Second", "Composer", emptyList())

        viewModel.movePendingSetListEntry(from = 0, to = 1)

        assertEquals(listOf("Second", "First"), viewModel.pendingSetListEntries.map { it.workTitle })
        assertEquals(listOf(1, 2), viewModel.pendingSetListEntries.map { it.order })
        coVerify(exactly = 0) { setListEntriesRepository.reorderSetListEntries(any()) }
    }

    @Test
    fun `replacePendingSetListEntry preserves the existing entry order`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.addPendingSetListEntry("w1", "First", "Composer", emptyList())
        viewModel.addPendingSetListEntry("w2", "Second", "Composer", emptyList())
        val secondId = viewModel.pendingSetListEntries[1].localId

        viewModel.replacePendingSetListEntry(secondId, "w9", "Replaced", "Composer", emptyList())

        val replaced = viewModel.pendingSetListEntries.single { it.localId == secondId }
        assertEquals("Replaced", replaced.workTitle)
        assertEquals(2, replaced.order)
    }

    @Test
    fun `savePerformance sends inline entries whose order matches the reordered pending list`() = runTest {
        val requestSlot = slot<PerformanceRequest>()
        coEvery { performancesRepository.createPerformance(capture(requestSlot)) } returns
            ApiResult.Success(performance())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.updateDraftDate(1_700_000_000_000L)
        viewModel.updateDraftVenue("v1", "Hall")
        viewModel.addPendingSetListEntry("w1", "First", "Composer", emptyList())
        viewModel.addPendingSetListEntry("w2", "Second", "Composer", emptyList())
        viewModel.movePendingSetListEntry(from = 0, to = 1)

        viewModel.savePerformance {}
        advanceUntilIdle()

        val setList = requestSlot.captured.setList
        assertEquals(listOf("w2", "w1"), setList.map { it.workId })
        assertEquals(listOf(1, 2), setList.map { it.order })
    }
}
