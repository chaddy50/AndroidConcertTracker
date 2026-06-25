package com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch

import androidx.lifecycle.SavedStateHandle
import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.MusicBrainzResult
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.MusicBrainzRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
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

    private val localPerformer = Performer(
        id = "p1",
        name = "Local Performer",
        type = PerformerType.SOLO,
        specialty = "piano",
        musicbrainzId = "mb-local"
    )
    private val apiResult = MusicBrainzResult(id = "mb-api", name = "Api Performer")

    private fun viewModel() = MusicBrainzSearchViewModel(
        SavedStateHandle(mapOf("entityType" to MusicBrainzEntityType.PERFORMER)),
        musicBrainzRepository,
        performersRepository
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
    fun `search merges results when both sources succeed`() = runTest {
        coEvery { performersRepository.searchPerformers(any()) } returns
            ApiResult.Success(listOf(localPerformer))
        coEvery { musicBrainzRepository.search(any(), any()) } returns
            ApiResult.Success(listOf(apiResult))
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(listOf("mb-local", "mb-api"), state.results.map { it.id })
    }

    @Test
    fun `search shows API results when local fails`() = runTest {
        coEvery { performersRepository.searchPerformers(any()) } returns
            ApiResult.Error(ApiErrorType.Type.NETWORK)
        coEvery { musicBrainzRepository.search(any(), any()) } returns
            ApiResult.Success(listOf(apiResult))
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(listOf("mb-api"), state.results.map { it.id })
    }

    @Test
    fun `search shows local results when API fails`() = runTest {
        coEvery { performersRepository.searchPerformers(any()) } returns
            ApiResult.Success(listOf(localPerformer))
        coEvery { musicBrainzRepository.search(any(), any()) } returns
            ApiResult.Error(ApiErrorType.Type.TIMEOUT)
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState as MusicBrainzSearchUiState.Results
        assertEquals(listOf("mb-local"), state.results.map { it.id })
    }

    @Test
    fun `search shows Error carrying errorType when both sources fail`() = runTest {
        coEvery { performersRepository.searchPerformers(any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        coEvery { musicBrainzRepository.search(any(), any()) } returns
            ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModel()

        viewModel.search()
        advanceUntilIdle()

        assertEquals(MusicBrainzSearchUiState.Error(ApiErrorType.Type.SERVER), viewModel.uiState)
    }
}
