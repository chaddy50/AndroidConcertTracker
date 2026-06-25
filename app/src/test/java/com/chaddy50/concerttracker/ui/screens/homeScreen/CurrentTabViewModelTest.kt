package com.chaddy50.concerttracker.ui.screens.homeScreen

import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab.CurrentTabUiState
import com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab.CurrentTabViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentTabViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PerformancesRepository = mockk()
    private val upcoming = Performance(
        id = "p1", date = "2024-06-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"), status = PerformanceStatus.UPCOMING
    )
    private val recent = Performance(
        id = "p2", date = "2024-05-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"), status = PerformanceStatus.ATTENDED
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
    fun `uiState is Loading immediately after loadData`() = runTest {
        coEvery { repository.getNextUpcomingPerformance() } returns ApiResult.Success(upcoming)
        coEvery { repository.getRecentlyAttendedPerformances() } returns ApiResult.Success(listOf(recent))
        val viewModel = CurrentTabViewModel(repository)
        assertTrue(viewModel.uiState is CurrentTabUiState.Loading)
    }

    @Test
    fun `transitions to Success with both fields when both calls return Success`() = runTest {
        coEvery { repository.getNextUpcomingPerformance() } returns ApiResult.Success(upcoming)
        coEvery { repository.getRecentlyAttendedPerformances() } returns ApiResult.Success(listOf(recent))
        val viewModel = CurrentTabViewModel(repository)
        advanceUntilIdle()
        val state = viewModel.uiState as CurrentTabUiState.Success
        assertEquals("p1", state.nextUpcoming?.id)
        assertEquals(1, state.recentAttended.size)
    }

    @Test
    fun `transitions to Success with null nextUpcoming when that call returns Success null`() = runTest {
        coEvery { repository.getNextUpcomingPerformance() } returns ApiResult.Success(null)
        coEvery { repository.getRecentlyAttendedPerformances() } returns ApiResult.Success(listOf(recent))
        val viewModel = CurrentTabViewModel(repository)
        advanceUntilIdle()
        val state = viewModel.uiState as CurrentTabUiState.Success
        assertNull(state.nextUpcoming)
        assertEquals(1, state.recentAttended.size)
    }

    @Test
    fun `transitions to Error when nextUpcoming call returns Error`() = runTest {
        coEvery { repository.getNextUpcomingPerformance() } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        coEvery { repository.getRecentlyAttendedPerformances() } returns ApiResult.Success(listOf(recent))
        val viewModel = CurrentTabViewModel(repository)
        advanceUntilIdle()
        assertEquals(CurrentTabUiState.Error(ApiErrorType.Type.NETWORK), viewModel.uiState)
    }

    @Test
    fun `transitions to Error when recentAttended call returns Error`() = runTest {
        coEvery { repository.getNextUpcomingPerformance() } returns ApiResult.Success(upcoming)
        coEvery { repository.getRecentlyAttendedPerformances() } returns ApiResult.Error(ApiErrorType.Type.TIMEOUT)
        val viewModel = CurrentTabViewModel(repository)
        advanceUntilIdle()
        assertEquals(CurrentTabUiState.Error(ApiErrorType.Type.TIMEOUT), viewModel.uiState)
    }

    @Test
    fun `retry succeeds after failure`() = runTest {
        coEvery { repository.getNextUpcomingPerformance() } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        coEvery { repository.getRecentlyAttendedPerformances() } returns ApiResult.Error(ApiErrorType.Type.SERVER)
        val viewModel = CurrentTabViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState is CurrentTabUiState.Error)
        coEvery { repository.getNextUpcomingPerformance() } returns ApiResult.Success(upcoming)
        coEvery { repository.getRecentlyAttendedPerformances() } returns ApiResult.Success(listOf(recent))
        viewModel.loadData()
        advanceUntilIdle()
        assertTrue(viewModel.uiState is CurrentTabUiState.Success)
    }
}
