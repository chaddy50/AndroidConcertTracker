package com.chaddy50.concerttracker.ui.screens.homeScreen

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab.CurrentTabUiState
import com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab.CurrentTabViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `uiState combines next upcoming and recently attended from Room`() = runTest {
        every { repository.observeNextUpcomingPerformance() } returns flowOf(upcoming)
        every { repository.observeRecentlyAttendedPerformances() } returns flowOf(listOf(recent))
        coEvery { repository.loadPerformances() } returns ApiResult.Success(Unit)
        val viewModel = CurrentTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CurrentTabUiState.Content)
        state as CurrentTabUiState.Content
        assertEquals("p1", state.nextUpcoming?.id)
        assertEquals(listOf("p2"), state.recentlyAttended.map { it.id })
    }

    @Test
    fun `uiState re-emits when a newly created performance appears in the Room flow`() = runTest {
        val nextFlow = MutableStateFlow<Performance?>(null)
        every { repository.observeNextUpcomingPerformance() } returns nextFlow
        every { repository.observeRecentlyAttendedPerformances() } returns flowOf(emptyList())
        coEvery { repository.loadPerformances() } returns ApiResult.Success(Unit)
        val viewModel = CurrentTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CurrentTabUiState.Empty)

        nextFlow.value = upcoming
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CurrentTabUiState.Content)
        assertEquals("p1", (state as CurrentTabUiState.Content).nextUpcoming?.id)
    }

    @Test
    fun `uiState shows cached content immediately, even while a refresh is in flight`() = runTest {
        every { repository.observeNextUpcomingPerformance() } returns flowOf(upcoming)
        every { repository.observeRecentlyAttendedPerformances() } returns flowOf(listOf(recent))
        val loadResult = CompletableDeferred<ApiResult<Unit>>()
        coEvery { repository.loadPerformances() } coAnswers { loadResult.await() }
        val viewModel = CurrentTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Offline-first: cached content wins over the in-flight refresh spinner.
        assertTrue(viewModel.uiState.value is CurrentTabUiState.Content)

        loadResult.complete(ApiResult.Success(Unit))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CurrentTabUiState.Content)
    }

    @Test
    fun `cached content is kept when the refresh fails offline`() = runTest {
        every { repository.observeNextUpcomingPerformance() } returns flowOf(upcoming)
        every { repository.observeRecentlyAttendedPerformances() } returns flowOf(listOf(recent))
        coEvery { repository.loadPerformances() } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = CurrentTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CurrentTabUiState.Content)
    }

    @Test
    fun `refresh failure with nothing cached shows Empty, not an error`() = runTest {
        every { repository.observeNextUpcomingPerformance() } returns flowOf(null)
        every { repository.observeRecentlyAttendedPerformances() } returns flowOf(emptyList())
        coEvery { repository.loadPerformances() } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = CurrentTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CurrentTabUiState.Empty)
    }
}
