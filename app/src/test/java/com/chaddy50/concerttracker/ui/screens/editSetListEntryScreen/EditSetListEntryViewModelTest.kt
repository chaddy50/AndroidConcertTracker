package com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.FeaturedPerformer
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
class EditSetListEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val performancesRepository: PerformancesRepository = mockk()
    private val setListEntriesRepository: SetListEntriesRepository = mockk()

    private fun work(id: String) = Work(id = id, title = "Title $id")

    private fun entry(id: String) = SetListEntry(
        id = id,
        work = work("w-$id"),
        order = 1,
        featuredPerformers = listOf(
            FeaturedPerformer(Performer("perf1", "Soloist", PerformerType.SOLO), role = "piano")
        )
    )

    private fun performance(setList: List<SetListEntry> = emptyList()) = Performance(
        id = "p1",
        date = "2024-06-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"),
        status = PerformanceStatus.UPCOMING,
        setList = setList
    )

    private fun createViewModel() = EditSetListEntryViewModel(
        SavedStateHandle(mapOf("performanceId" to "p1", "entryId" to null)),
        performancesRepository, setListEntriesRepository
    )

    private fun editViewModel() = EditSetListEntryViewModel(
        SavedStateHandle(mapOf("performanceId" to "p1", "entryId" to "e1")),
        performancesRepository, setListEntriesRepository
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
    fun `loadData populates drafts from existing entry on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            performance(setList = listOf(entry("e1")))
        val viewModel = editViewModel()
        advanceUntilIdle()

        assertEquals(SetListEntryEditUiState.Ready, viewModel.uiState)
        assertEquals("w-e1", viewModel.draftWorkId)
        assertEquals("Title w-e1", viewModel.draftWorkTitle)
        assertEquals(listOf("perf1"), viewModel.draftFeaturedPerformers.map { it.performerId })
    }

    @Test
    fun `loadData shows NotFound when the performance is not cached`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns null
        val viewModel = editViewModel()
        advanceUntilIdle()
        assertEquals(SetListEntryEditUiState.NotFound, viewModel.uiState)
    }

    @Test
    fun `selectWork references the already-materialized work with no network create`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectWork("w99", "Title w99", "Composer")
        assertEquals("w99", viewModel.draftWorkId)
        assertEquals("Title w99", viewModel.draftWorkTitle)
        assertEquals("Composer", viewModel.draftComposerName)
        assertNull(viewModel.saveError)
    }

    @Test
    fun `addDraftFeaturedPerformer adds the already-materialized performer and skips duplicates`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addDraftFeaturedPerformer("perf2", "Cellist", "SOLO", null)
        assertEquals(listOf("perf2"), viewModel.draftFeaturedPerformers.map { it.performerId })

        viewModel.addDraftFeaturedPerformer("perf2", "Cellist", "SOLO", null)
        assertEquals(1, viewModel.draftFeaturedPerformers.size)
    }

    @Test
    fun `saveSetListEntry creates entry and invokes onSaved on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
        coEvery { setListEntriesRepository.createSetListEntry(any()) } returns
            ApiResult.Success(entry("e9"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.selectWork("w99", "Title w99", "Composer")
        advanceUntilIdle()

        var saved = false
        viewModel.saveSetListEntry(onSaved = { saved = true })
        advanceUntilIdle()

        assertTrue(saved)
        assertFalse(viewModel.isSaving)
        coVerify(exactly = 1) { setListEntriesRepository.createSetListEntry(any()) }
    }

    @Test
    fun `saveSetListEntry sets saveError on Error`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns performance()
        coEvery { setListEntriesRepository.createSetListEntry(any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.selectWork("w99", "Title w99", "Composer")
        advanceUntilIdle()

        var saved = false
        viewModel.saveSetListEntry(onSaved = { saved = true })
        advanceUntilIdle()

        assertFalse(saved)
        assertFalse(viewModel.isSaving)
        assertNotNull(viewModel.saveError)
    }

    @Test
    fun `deleteSetListEntry invokes onDeleted on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            performance(setList = listOf(entry("e1")))
        coEvery { setListEntriesRepository.deleteSetListEntry("e1") } returns ApiResult.Success(Unit)
        val viewModel = editViewModel()
        advanceUntilIdle()

        var deleted = false
        viewModel.deleteSetListEntry { deleted = true }
        advanceUntilIdle()
        assertTrue(deleted)
        assertFalse(viewModel.isDeleting)
    }

    @Test
    fun `deleteSetListEntry sets saveError on Error`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            performance(setList = listOf(entry("e1")))
        coEvery { setListEntriesRepository.deleteSetListEntry("e1") } returns
            ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = editViewModel()
        advanceUntilIdle()

        var deleted = false
        viewModel.deleteSetListEntry { deleted = true }
        advanceUntilIdle()
        assertFalse(deleted)
        assertNotNull(viewModel.saveError)
    }
}
