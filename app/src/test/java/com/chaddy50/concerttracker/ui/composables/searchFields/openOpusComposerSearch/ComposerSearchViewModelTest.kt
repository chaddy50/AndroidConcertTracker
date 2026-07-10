package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.repository.ComposersRepository
import com.chaddy50.concerttracker.data.repository.OpenOpusRepository
import io.mockk.coVerify
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val openOpusRepository: OpenOpusRepository = mockk()
    private val composersRepository: ComposersRepository = mockk()

    private val catalogComposer = OpenOpusComposer(id = "oo1", name = "Mozart", completeName = "Wolfgang Mozart")

    private fun viewModel() = ComposerSearchViewModel(openOpusRepository, composersRepository)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { composersRepository.searchComposers(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search surfaces online composers as catalog rows`() = runTest {
        coEvery { openOpusRepository.searchComposers(any()) } returns ApiResult.Success(listOf(catalogComposer))
        val viewModel = viewModel()
        viewModel.updateSearchQuery("mozart")

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as ComposerSearchUiState.Results
        assertEquals(listOf("oo1"), state.rows.filterIsInstance<ComposerSearchResult.FromApi>().map { it.composer.id })
    }

    @Test
    fun `cached composers merge with online results and de-duplicate by Open Opus id`() = runTest {
        every { composersRepository.searchComposers(any()) } returns flowOf(
            listOf(
                Composer(id = "l1", name = "Mozart Dup", openOpusId = "oo1"),
                Composer(id = "l2", name = "Local Only", openOpusId = null)
            )
        )
        coEvery { openOpusRepository.searchComposers(any()) } returns ApiResult.Success(listOf(catalogComposer))
        val viewModel = viewModel()
        viewModel.updateSearchQuery("mozart")

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as ComposerSearchUiState.Results
        assertEquals(listOf("l1", "l2"), state.rows.filterIsInstance<ComposerSearchResult.Local>().map { it.composer.id })
        assertTrue(state.rows.filterIsInstance<ComposerSearchResult.FromApi>().isEmpty())
    }

    @Test
    fun `cached composers stay in the list when the online search errors offline`() = runTest {
        every { composersRepository.searchComposers(any()) } returns flowOf(
            listOf(Composer(id = "l2", name = "Local Only"))
        )
        coEvery { openOpusRepository.searchComposers(any()) } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModel()
        viewModel.updateSearchQuery("mozart")

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as ComposerSearchUiState.Results
        assertEquals(listOf("l2"), state.rows.filterIsInstance<ComposerSearchResult.Local>().map { it.composer.id })
    }

    @Test
    fun `search shows Error carrying errorType when nothing is cached and online fails`() = runTest {
        coEvery { openOpusRepository.searchComposers(any()) } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = viewModel()
        viewModel.updateSearchQuery("mozart")

        viewModel.search()
        advanceUntilIdle()

        assertEquals(ComposerSearchUiState.Error(ApiErrorType.Type.SERVER), viewModel.uiState)
    }

    @Test
    fun `search returns early on blank query`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.search()
        advanceUntilIdle()

        assertTrue(viewModel.uiState is ComposerSearchUiState.Idle)
        coVerify(exactly = 0) { openOpusRepository.searchComposers(any()) }
    }
}
