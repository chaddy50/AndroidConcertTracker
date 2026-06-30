package com.chaddy50.concerttracker.ui.screens.homeScreen

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastTabUiState
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastTabViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class PastTabViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PerformancesRepository = mockk()
    private val performance = Performance(
        id = "p1", date = "2024-01-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"), status = PerformanceStatus.ATTENDED
    )

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `uiState emits past performances from Room`() = runTest {
        every { repository.observePastPerformances() } returns flowOf(listOf(performance))
        coEvery { repository.loadPerformances() } returns ApiResult.Success(Unit)
        val viewModel = PastTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PastTabUiState.Content)
        assertEquals(listOf("p1"), (state as PastTabUiState.Content).performances.map { it.id })
    }

    @Test
    fun `uiState is Loading while a refresh is in flight, even when content is cached`() = runTest {
        every { repository.observePastPerformances() } returns flowOf(listOf(performance))
        val loadResult = CompletableDeferred<ApiResult<Unit>>()
        coEvery { repository.loadPerformances() } coAnswers { loadResult.await() }
        val viewModel = PastTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // The load is still suspended, so Loading takes priority over the cached content.
        assertTrue(viewModel.uiState.value is PastTabUiState.Loading)

        loadResult.complete(ApiResult.Success(Unit))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PastTabUiState.Content)
    }

    @Test
    fun `init loadPerformances failure is surfaced as Error`() = runTest {
        every { repository.observePastPerformances() } returns flowOf(emptyList())
        coEvery { repository.loadPerformances() } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = PastTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PastTabUiState.Error)
        assertEquals(ApiErrorType.Type.SERVER, (state as PastTabUiState.Error).errorType)
    }
}
