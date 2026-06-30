package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.repository.ComposersRepository
import com.chaddy50.concerttracker.data.repository.OpenOpusRepository
import io.mockk.coVerify
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val openOpusRepository: OpenOpusRepository = mockk()
    private val composersRepository: ComposersRepository = mockk()

    private fun viewModel() = ComposerSearchViewModel(openOpusRepository, composersRepository)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search merges and dedupes results when both sources succeed`() = runTest {
        coEvery { openOpusRepository.searchComposers(any()) } returns
            ApiResult.Success(listOf(OpenOpusComposer(id = "oo1", name = "Mozart", completeName = "Wolfgang Mozart")))
        coEvery { composersRepository.searchComposers(any()) } returns
            ApiResult.Success(
                listOf(
                    Composer(id = "l1", name = "Mozart Dup", openOpusId = "oo1"),
                    Composer(id = "l2", name = "Local Only", openOpusId = null)
                )
            )
        val viewModel = viewModel()
        viewModel.updateSearchQuery("mozart")

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as ComposerSearchUiState.Results
        assertEquals(listOf("Local Only", "Wolfgang Mozart"), state.composers.map { it.completeName })
    }

    @Test
    fun `search degrades to other source when one fails`() = runTest {
        coEvery { composersRepository.searchComposers(any()) } returns
            ApiResult.Error(ApiErrorType.Type.NETWORK)
        coEvery { openOpusRepository.searchComposers(any()) } returns
            ApiResult.Success(listOf(OpenOpusComposer(id = "oo1", name = "Mozart", completeName = "Wolfgang Mozart")))
        val viewModel = viewModel()
        viewModel.updateSearchQuery("mozart")

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as ComposerSearchUiState.Results
        assertEquals(listOf("oo1"), state.composers.map { it.id })
    }

    @Test
    fun `search shows Error carrying errorType when both sources fail`() = runTest {
        coEvery { composersRepository.searchComposers(any()) } returns
            ApiResult.Error(ApiErrorType.Type.TIMEOUT)
        coEvery { openOpusRepository.searchComposers(any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = viewModel()
        viewModel.updateSearchQuery("mozart")

        viewModel.search()
        advanceUntilIdle()

        assertEquals(ComposerSearchUiState.Error(ApiErrorType.Type.TIMEOUT), viewModel.uiState)
    }

    @Test
    fun `search returns early on blank query`() = runTest {
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        assertTrue(viewModel.uiState is ComposerSearchUiState.Idle)
        coVerify(exactly = 0) { composersRepository.searchComposers(any()) }
        coVerify(exactly = 0) { openOpusRepository.searchComposers(any()) }
    }
}
