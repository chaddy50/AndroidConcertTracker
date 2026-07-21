package com.chaddy50.concerttracker.ui.screens.homeScreen

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab.UpcomingListItem
import com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab.UpcomingTabUiState
import com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab.UpcomingTabViewModel
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
class UpcomingTabViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PerformancesRepository = mockk()
    private val performance = Performance(
        id = "p1", date = "2024-06-01T19:00:00Z",
        venue = Venue("v1", "Hall", "123", "way"), status = PerformanceStatus.UPCOMING
    )

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `uiState emits upcoming performances from Room`() = runTest {
        every { repository.observeUpcomingPerformances() } returns flowOf(listOf(performance))
        coEvery { repository.loadPerformances() } returns ApiResult.Success(Unit)
        val viewModel = UpcomingTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UpcomingTabUiState.Content)
        val ids = (state as UpcomingTabUiState.Content).items.filterIsInstance<UpcomingListItem.Entry>().map { it.performance.id }
        assertEquals(listOf("p1"), ids)
    }

    @Test
    fun `uiState re-emits when the upcoming list grows`() = runTest {
        val flow = MutableStateFlow<List<Performance>>(emptyList())
        every { repository.observeUpcomingPerformances() } returns flow
        coEvery { repository.loadPerformances() } returns ApiResult.Success(Unit)
        val viewModel = UpcomingTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UpcomingTabUiState.Empty)

        flow.value = listOf(performance)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UpcomingTabUiState.Content)
        assertEquals(1, (state as UpcomingTabUiState.Content).items.filterIsInstance<UpcomingListItem.Entry>().size)
    }

    @Test
    fun `uiState shows cached content immediately, even while a refresh is in flight`() = runTest {
        every { repository.observeUpcomingPerformances() } returns flowOf(listOf(performance))
        val loadResult = CompletableDeferred<ApiResult<Unit>>()
        coEvery { repository.loadPerformances() } coAnswers { loadResult.await() }
        val viewModel = UpcomingTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Offline-first: cached content wins over the in-flight refresh spinner.
        assertTrue(viewModel.uiState.value is UpcomingTabUiState.Content)

        loadResult.complete(ApiResult.Success(Unit))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UpcomingTabUiState.Content)
    }

    @Test
    fun `cached content is kept when the refresh fails offline`() = runTest {
        every { repository.observeUpcomingPerformances() } returns flowOf(listOf(performance))
        coEvery { repository.loadPerformances() } returns ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = UpcomingTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UpcomingTabUiState.Content)
        val ids = (state as UpcomingTabUiState.Content).items.filterIsInstance<UpcomingListItem.Entry>().map { it.performance.id }
        assertEquals(listOf("p1"), ids)
    }

    @Test
    fun `refresh failure with nothing cached shows Empty, not an error`() = runTest {
        every { repository.observeUpcomingPerformances() } returns flowOf(emptyList())
        coEvery { repository.loadPerformances() } returns ApiResult.Error(ApiErrorType.Type.TIMEOUT)
        val viewModel = UpcomingTabViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UpcomingTabUiState.Empty)
    }
}
