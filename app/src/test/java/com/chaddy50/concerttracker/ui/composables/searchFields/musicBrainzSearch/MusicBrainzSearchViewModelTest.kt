package com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.MusicBrainzResult
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.MusicBrainzRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicBrainzSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val musicBrainzRepository: MusicBrainzRepository = mockk()
    private val performersRepository: PerformersRepository = mockk()

    private val cachedPerformer = Performer(
        id = "p1",
        name = "Local Performer",
        type = PerformerType.SOLO,
        specialty = "piano",
        musicbrainzId = "mb-local"
    )
    private val catalogResult = MusicBrainzResult(id = "mb-api", name = "Api Performer", performerType = PerformerType.SOLO)

    private fun viewModel() = MusicBrainzSearchViewModel(
        musicBrainzRepository,
        performersRepository
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { performersRepository.searchPerformers(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search surfaces online results as catalog rows`() = runTest {
        coEvery { musicBrainzRepository.search(any()) } returns ApiResult.Success(listOf(catalogResult))
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(listOf("mb-api"), state.rows.filterIsInstance<PerformerSearchResult.FromApi>().map { it.result.id })
    }

    @Test
    fun `cached performers merge with online results and de-duplicate by MusicBrainz id`() = runTest {
        every { performersRepository.searchPerformers(any()) } returns flowOf(listOf(cachedPerformer))
        coEvery { musicBrainzRepository.search(any()) } returns
            ApiResult.Success(listOf(catalogResult, MusicBrainzResult(id = "mb-local", name = "dupe")))
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(listOf("p1"), state.rows.filterIsInstance<PerformerSearchResult.Local>().map { it.performer.id })
        assertEquals(listOf("mb-api"), state.rows.filterIsInstance<PerformerSearchResult.FromApi>().map { it.result.id })
    }

    @Test
    fun `cached conductors are included in the performer list`() = runTest {
        every { performersRepository.searchPerformers(any()) } returns flowOf(
            listOf(cachedPerformer, cachedPerformer.copy(id = "p2", type = PerformerType.CONDUCTOR))
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(
            listOf("p1", "p2"),
            state.rows.filterIsInstance<PerformerSearchResult.Local>().map { it.performer.id }
        )
    }

    @Test
    fun `cached performers of every type appear as local rows`() = runTest {
        val performers = PerformerType.entries.mapIndexed { index, type ->
            cachedPerformer.copy(id = "p$index", type = type, musicbrainzId = null)
        }
        every { performersRepository.searchPerformers(any()) } returns flowOf(performers)
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(
            performers.map { it.id },
            state.rows.filterIsInstance<PerformerSearchResult.Local>().map { it.performer.id }
        )
    }

    @Test
    fun `search transitions to Empty when no results and nothing cached`() = runTest {
        coEvery { musicBrainzRepository.search(any()) } returns ApiResult.Success(emptyList())
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        assertEquals(MusicBrainzSearchUiState.Empty, viewModel.uiState)
    }

    @Test
    fun `search transitions to Error when it fails and nothing cached`() = runTest {
        coEvery { musicBrainzRepository.search(any()) } returns ApiResult.Error(ApiErrorType.Type.TIMEOUT)
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        assertEquals(MusicBrainzSearchUiState.Error(ApiErrorType.Type.TIMEOUT), viewModel.uiState)
    }

    @Test
    fun `cached performers stay in the list when the online search errors offline`() = runTest {
        every { performersRepository.searchPerformers(any()) } returns flowOf(listOf(cachedPerformer))
        coEvery { musicBrainzRepository.search(any()) } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(listOf("p1"), state.rows.filterIsInstance<PerformerSearchResult.Local>().map { it.performer.id })
    }

    @Test
    fun `selecting a cached performer returns it without creating`() = runTest {
        every { performersRepository.searchPerformers(any()) } returns flowOf(listOf(cachedPerformer))
        val viewModel = viewModel()
        advanceUntilIdle()

        var selected: Performer? = null
        viewModel.selectPerformer(cachedPerformer) { selected = it }

        assertEquals(cachedPerformer, selected)
        coVerify(exactly = 0) { performersRepository.findOrCreatePerformer(any()) }
    }

    @Test
    fun `selecting a catalog performer materializes it via findOrCreatePerformer`() = runTest {
        val materialized = Performer("p9", "Api Performer", PerformerType.SOLO, musicbrainzId = "mb-api")
        coEvery { performersRepository.findOrCreatePerformer(any()) } returns ApiResult.Success(materialized)
        val viewModel = viewModel()

        var selected: Performer? = null
        viewModel.selectPerformerFromApi(catalogResult) { selected = it }
        advanceUntilIdle()

        assertEquals(materialized, selected)
        assertNull(viewModel.saveError)
        coVerify(exactly = 1) { performersRepository.findOrCreatePerformer(any()) }
    }

    @Test
    fun `selecting a catalog performer sets saveError on Error`() = runTest {
        coEvery { performersRepository.findOrCreatePerformer(any()) } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = viewModel()

        var selected: Performer? = null
        viewModel.selectPerformerFromApi(catalogResult) { selected = it }
        advanceUntilIdle()

        assertNull(selected)
        assertEquals(false, viewModel.isSaving)
        assertEquals(ApiErrorType.Type.SERVER.toUserMessage(), viewModel.saveError)
    }

    @Test
    fun `creating a custom performer resolves with no external id`() = runTest {
        val materialized = Performer("p10", "Custom", PerformerType.OTHER)
        coEvery { performersRepository.findOrCreatePerformer(any()) } returns ApiResult.Success(materialized)
        val viewModel = viewModel()

        var selected: Performer? = null
        viewModel.createCustomPerformer("Custom", PerformerType.OTHER, null) { selected = it }
        advanceUntilIdle()

        assertEquals(materialized, selected)
        coVerify(exactly = 1) {
            performersRepository.findOrCreatePerformer(match { it.musicbrainzId == null && it.name == "Custom" })
        }
    }
}
