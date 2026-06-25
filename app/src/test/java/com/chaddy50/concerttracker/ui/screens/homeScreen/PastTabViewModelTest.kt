package com.chaddy50.concerttracker.ui.screens.homeScreen

import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastTabUiState
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastTabViewModel
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
class PastTabViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PerformancesRepository = mockk()
    private val performance = Performance(
        id = "p1",
        date = "2024-01-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"),
        status = PerformanceStatus.ATTENDED
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
    fun `uiState transitions through Loading then Success on successful load`() = runTest {
        coEvery { repository.getPastPerformances() } returns ApiResult.Success(listOf(performance))
        val viewModel = PastTabViewModel(repository)
        assertTrue(viewModel.uiState is PastTabUiState.Loading)
        advanceUntilIdle()
        assertTrue(viewModel.uiState is PastTabUiState.Success)
        assertEquals(1, (viewModel.uiState as PastTabUiState.Success).performances.size)
    }

    @Test
    fun `uiState transitions to Error on failure`() = runTest {
        coEvery { repository.getPastPerformances() } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = PastTabViewModel(repository)
        advanceUntilIdle()
        assertEquals(PastTabUiState.Error(ApiErrorType.Type.NETWORK), viewModel.uiState)
    }

    @Test
    fun `retry succeeds after failure`() = runTest {
        coEvery { repository.getPastPerformances() } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = PastTabViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState is PastTabUiState.Error)
        coEvery { repository.getPastPerformances() } returns ApiResult.Success(listOf(performance))
        viewModel.loadData()
        advanceUntilIdle()
        assertTrue(viewModel.uiState is PastTabUiState.Success)
    }
}
