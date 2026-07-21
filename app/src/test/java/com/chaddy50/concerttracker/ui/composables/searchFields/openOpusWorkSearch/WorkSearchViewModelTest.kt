package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusWork
import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.repository.OpenOpusRepository
import com.chaddy50.concerttracker.data.repository.WorksRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class WorkSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val openOpusRepository: OpenOpusRepository = mockk()
    private val worksRepository: WorksRepository = mockk()

    private val catalogWork = OpenOpusWork(id = "w1", title = "Symphony No. 5", genre = "Orchestral")

    private fun viewModel(
        composerEntityId: String? = null,
        composerOpenOpusId: String? = "oo1"
    ) = WorkSearchViewModel(
        SavedStateHandle(
            mapOf(
                "composerEntityId" to composerEntityId,
                "composerOpenOpusId" to composerOpenOpusId,
                "composerName" to "Beethoven"
            )
        ),
        openOpusRepository,
        worksRepository
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { worksRepository.searchWorksForComposer(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `catalog works are fetched and surfaced as catalog rows`() = runTest {
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(listOf(catalogWork))
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(listOf("w1"), state.rows.filterIsInstance<WorkSearchResult.FromApi>().map { it.work.id })
    }

    @Test
    fun `cached works merge with catalog works and de-duplicate by Open Opus id`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Renamed locally", composers = listOf(Composer("c1", "Beethoven")), openOpusId = "w1"))
        )
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(listOf(catalogWork))
        val viewModel = viewModel(composerEntityId = "c1")

        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(listOf("cached-1"), state.rows.filterIsInstance<WorkSearchResult.Local>().map { it.work.id })
        assertTrue(state.rows.filterIsInstance<WorkSearchResult.FromApi>().isEmpty())
    }

    @Test
    fun `null composerOpenOpusId does not fetch catalog works`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Custom Work"))
        )
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)

        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(listOf("cached-1"), state.rows.filterIsInstance<WorkSearchResult.Local>().map { it.work.id })
        coVerify(exactly = 0) { openOpusRepository.getWorksByComposer(any()) }
    }

    @Test
    fun `null composerEntityId does not query cached works`() = runTest {
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(listOf(catalogWork))
        val viewModel = viewModel(composerEntityId = null)

        advanceUntilIdle()

        coVerify(exactly = 0) { worksRepository.searchWorksForComposer(any(), any()) }
    }

    @Test
    fun `catalog fetch error surfaces Error when nothing cached`() = runTest {
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(WorkSearchUiState.Error(ApiErrorType.Type.NETWORK), viewModel.uiState)
    }

    @Test
    fun `selecting a cached work returns it without resolving`() = runTest {
        val cached = Work(id = "cached-1", title = "Symphony No. 5")
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(listOf(cached))
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)
        advanceUntilIdle()

        var selected: Work? = null
        viewModel.selectWork(cached) { selected = it }

        assertEquals(cached, selected)
        coVerify(exactly = 0) { worksRepository.findOrCreateWork(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `selecting a catalog work materializes it via findOrCreateWork`() = runTest {
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(listOf(catalogWork))
        val materialized = Work(id = "w-our", title = "Symphony No. 5")
        coEvery { worksRepository.findOrCreateWork("w1", "Symphony No. 5", null, "oo1", "Beethoven", "Orchestral") } returns
            ApiResult.Success(materialized)
        val viewModel = viewModel()
        advanceUntilIdle()

        var selected: Work? = null
        viewModel.selectWorkFromApi(catalogWork) { selected = it }
        advanceUntilIdle()

        assertEquals(materialized, selected)
        assertNull(viewModel.saveError)
    }

    // --- Genre filtering for local works ---

    @Test
    fun `local works are filtered out when their genre does not match the selected genre`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Piano Sonata", genre = "Keyboard"))
        )
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.ORCHESTRAL)
        advanceUntilIdle()

        assertEquals(WorkSearchUiState.Empty, viewModel.uiState)
    }

    @Test
    fun `local works with matching genre are kept when a genre filter is active`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(
                Work(id = "cached-1", title = "Symphony", genre = "Orchestral"),
                Work(id = "cached-2", title = "Sonata", genre = "Keyboard")
            )
        )
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.ORCHESTRAL)
        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(listOf("cached-1"), state.rows.filterIsInstance<WorkSearchResult.Local>().map { it.work.id })
    }

    @Test
    fun `ALL genre shows all local works regardless of their genre`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(
                Work(id = "cached-1", title = "Symphony", genre = "Orchestral"),
                Work(id = "cached-2", title = "Sonata", genre = "Keyboard"),
                Work(id = "cached-3", title = "Untitled", genre = null)
            )
        )
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)
        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(3, state.rows.size)
    }

    @Test
    fun `local works with null genre are excluded when a specific genre filter is active`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Untitled", genre = null))
        )
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.VOCAL)
        advanceUntilIdle()

        assertEquals(WorkSearchUiState.Empty, viewModel.uiState)
    }

    @Test
    fun `genre matching for local works is case-insensitive`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Symphony", genre = "orchestral"))
        )
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.ORCHESTRAL)
        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(1, state.rows.size)
    }

    @Test
    fun `switching genre filters updates local work visibility`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(
                Work(id = "cached-1", title = "Concerto", genre = "Orchestral"),
                Work(id = "cached-2", title = "Nocturne", genre = "Keyboard")
            )
        )
        val viewModel = viewModel(composerEntityId = "c1", composerOpenOpusId = null)
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.ORCHESTRAL)
        advanceUntilIdle()
        var state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(listOf("cached-1"), state.rows.filterIsInstance<WorkSearchResult.Local>().map { it.work.id })

        viewModel.selectGenre(OpenOpusGenre.KEYBOARD)
        advanceUntilIdle()
        state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(listOf("cached-2"), state.rows.filterIsInstance<WorkSearchResult.Local>().map { it.work.id })

        viewModel.selectGenre(OpenOpusGenre.ALL)
        advanceUntilIdle()
        state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(2, state.rows.size)
    }

    @Test
    fun `genre filter applies consistently to both local and API works`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Local Orchestral", genre = "Orchestral"))
        )
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(
            listOf(
                OpenOpusWork(id = "api-1", title = "API Keyboard", genre = "Keyboard"),
                OpenOpusWork(id = "api-2", title = "API Orchestral", genre = "Orchestral")
            )
        )
        val viewModel = viewModel(composerEntityId = "c1")
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.ORCHESTRAL)
        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(1, state.rows.filterIsInstance<WorkSearchResult.Local>().size)
        assertEquals(1, state.rows.filterIsInstance<WorkSearchResult.FromApi>().size)
        assertEquals(2, state.rows.size)
    }

    @Test
    fun `de-duplication still works correctly after genre filtering of local works`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Symphony", genre = "Orchestral", openOpusId = "w1"))
        )
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(
            listOf(OpenOpusWork(id = "w1", title = "Symphony", genre = "Orchestral"))
        )
        val viewModel = viewModel(composerEntityId = "c1")
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.ORCHESTRAL)
        advanceUntilIdle()

        val state = viewModel.uiState as WorkSearchUiState.Results
        assertEquals(1, state.rows.size)
        assertTrue(state.rows.first() is WorkSearchResult.Local)
    }

    @Test
    fun `local work filtered by genre does not cause its API duplicate to reappear`() = runTest {
        every { worksRepository.searchWorksForComposer("c1", any()) } returns flowOf(
            listOf(Work(id = "cached-1", title = "Nocturne", genre = "Keyboard", openOpusId = "w1"))
        )
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(
            listOf(OpenOpusWork(id = "w1", title = "Nocturne", genre = "Keyboard"))
        )
        val viewModel = viewModel(composerEntityId = "c1")
        advanceUntilIdle()

        viewModel.selectGenre(OpenOpusGenre.ORCHESTRAL)
        advanceUntilIdle()

        assertEquals(WorkSearchUiState.Empty, viewModel.uiState)
    }

    // --- End genre filtering tests ---

    @Test
    fun `creating a custom work resolves with a null Open Opus work id`() = runTest {
        coEvery { openOpusRepository.getWorksByComposer("oo1") } returns ApiResult.Success(emptyList())
        val materialized = Work(id = "w-custom", title = "My Work")
        coEvery { worksRepository.findOrCreateWork(null, "My Work", null, "oo1", "Beethoven") } returns
            ApiResult.Success(materialized)
        val viewModel = viewModel()
        advanceUntilIdle()

        var selected: Work? = null
        viewModel.createCustomWork("My Work") { selected = it }
        advanceUntilIdle()

        assertEquals(materialized, selected)
        coVerify(exactly = 1) { worksRepository.findOrCreateWork(null, "My Work", null, "oo1", "Beethoven") }
    }
}
