package com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.entity.FeaturedPerformer
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.data.repository.WorksRepository
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
    private val performersRepository: PerformersRepository = mockk()
    private val worksRepository: WorksRepository = mockk()
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
        performancesRepository, performersRepository, worksRepository, setListEntriesRepository
    )

    private fun editViewModel() = EditSetListEntryViewModel(
        SavedStateHandle(mapOf("performanceId" to "p1", "entryId" to "e1")),
        performancesRepository, performersRepository, worksRepository, setListEntriesRepository
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
            ApiResult.Success(performance(setList = listOf(entry("e1"))))
        val viewModel = editViewModel()
        advanceUntilIdle()

        assertEquals(SetListEntryEditUiState.Ready, viewModel.uiState)
        assertEquals("w-e1", viewModel.draftWorkId)
        assertEquals("Title w-e1", viewModel.draftWorkTitle)
        assertEquals(listOf("perf1"), viewModel.draftFeaturedPerformers.map { it.performerId })
    }

    @Test
    fun `loadData shows Error with errorType on failure`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns
            ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = editViewModel()
        advanceUntilIdle()
        assertEquals(SetListEntryEditUiState.Error(ApiErrorType.Type.NETWORK), viewModel.uiState)
    }

    @Test
    fun `selectWork sets draft fields on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        coEvery { worksRepository.createWorkFromOpenOpus(any(), any(), any(), any()) } returns
            ApiResult.Success(work("w99"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectWork("oo-work", "Title w99", "oo-composer", "Composer")
        advanceUntilIdle()
        assertEquals("w99", viewModel.draftWorkId)
        assertEquals("Title w99", viewModel.draftWorkTitle)
        assertEquals("Composer", viewModel.draftComposerName)
    }

    @Test
    fun `selectWork sets saveError on Error`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        coEvery { worksRepository.createWorkFromOpenOpus(any(), any(), any(), any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectWork("oo-work", "Title", "oo-composer", "Composer")
        advanceUntilIdle()
        assertNotNull(viewModel.saveError)
    }

    @Test
    fun `addDraftFeaturedPerformer adds on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        coEvery { performersRepository.createPerformer(any()) } returns
            ApiResult.Success(Performer("perf2", "Cellist", PerformerType.SOLO))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addDraftFeaturedPerformer("mb2", "Cellist", "SOLO", null)
        advanceUntilIdle()
        assertEquals(listOf("perf2"), viewModel.draftFeaturedPerformers.map { it.performerId })
    }

    @Test
    fun `addDraftFeaturedPerformer sets saveError on Error`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        coEvery { performersRepository.createPerformer(any()) } returns
            ApiResult.Error(ApiErrorType.Type.CLIENT)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.addDraftFeaturedPerformer("mb2", "Cellist", "SOLO", null)
        advanceUntilIdle()
        assertTrue(viewModel.draftFeaturedPerformers.isEmpty())
        assertNotNull(viewModel.saveError)
    }

    @Test
    fun `saveSetListEntry creates entry and invokes onSaved on Success`() = runTest {
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        coEvery { worksRepository.createWorkFromOpenOpus(any(), any(), any(), any()) } returns
            ApiResult.Success(work("w99"))
        coEvery { setListEntriesRepository.createSetListEntry(any()) } returns
            ApiResult.Success(entry("e9"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.selectWork("oo", "Title w99", "ooc", "Composer")
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
        coEvery { performancesRepository.getPerformance("p1") } returns ApiResult.Success(performance())
        coEvery { worksRepository.createWorkFromOpenOpus(any(), any(), any(), any()) } returns
            ApiResult.Success(work("w99"))
        coEvery { setListEntriesRepository.createSetListEntry(any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.selectWork("oo", "Title w99", "ooc", "Composer")
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
            ApiResult.Success(performance(setList = listOf(entry("e1"))))
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
            ApiResult.Success(performance(setList = listOf(entry("e1"))))
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
