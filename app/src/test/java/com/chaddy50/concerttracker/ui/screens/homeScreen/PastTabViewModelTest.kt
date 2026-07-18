package com.chaddy50.concerttracker.ui.screens.homeScreen

import androidx.paging.PagingData
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastTabViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The Past tab's content is a thin, cached transformation of the repository's paged flow: each
 * `Performance` is wrapped as a [com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastListItem.Entry]
 * and year headers are injected by `pastListSeparator`. The interesting behavior lives in
 * `PastListItemTest` (grouping) and `PerformanceDaoTest` (paging/ordering); these tests only
 * cover the ViewModel's wiring, since `cachedIn(viewModelScope)` is a hot cache a unit test can't
 * drain through `asSnapshot` without leaking its sharing coroutine onto the test scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PastTabViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PerformancesRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.loadPerformances() } returns ApiResult.Success(Unit)
        every { repository.observePastPerformancesPaged() } returns
            flowOf(PagingData.from(listOf(performance("p1", "2024-11-15T12:00:00Z"))))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun performance(id: String, date: String) = Performance(
        id = id, date = date,
        venue = Venue("v1", "Hall", "o", "way"), status = PerformanceStatus.ATTENDED
    )

    @Test
    fun `pagedItems is backed by the repository's paged past source`() = runTest {
        val viewModel = PastTabViewModel(repository)

        // Building the flow subscribes to the repository's paged source exactly once (cachedIn shares it).
        viewModel.pagedItems
        verify(exactly = 1) { repository.observePastPerformancesPaged() }
    }

    @Test
    fun `init kicks a single background sync`() = runTest {
        PastTabViewModel(repository)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.loadPerformances() }
    }
}
