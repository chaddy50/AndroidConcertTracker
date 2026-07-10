package com.chaddy50.concerttracker.ui.composables.searchFields.nominatimSearch

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.NominatimResult
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.repository.NominatimRepository
import com.chaddy50.concerttracker.data.repository.VenuesRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NominatimSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val nominatimRepository: NominatimRepository = mockk()
    private val venuesRepository: VenuesRepository = mockk()

    private val result = NominatimResult(osmId = 42L, osmType = "way", displayName = "Hall, City", name = "Hall")
    private val venue = Venue("v1", "Hall", "42", "way")

    private fun viewModel() = NominatimSearchViewModel(nominatimRepository, venuesRepository)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { venuesRepository.searchVenues(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search surfaces online results as catalog rows`() = runTest {
        coEvery { nominatimRepository.searchVenues(any()) } returns ApiResult.Success(listOf(result))
        val viewModel = viewModel()
        viewModel.search()
        advanceUntilIdle()
        val state = viewModel.uiState as CreateVenueUiState.Results
        assertEquals(listOf(42L), state.rows.filterIsInstance<VenueSearchResult.FromApi>().map { it.result.osmId })
    }

    @Test
    fun `search transitions to Empty when no results and nothing cached`() = runTest {
        coEvery { nominatimRepository.searchVenues(any()) } returns ApiResult.Success(emptyList())
        val viewModel = viewModel()
        viewModel.search()
        advanceUntilIdle()
        assertTrue(viewModel.uiState is CreateVenueUiState.Empty)
    }

    @Test
    fun `search transitions to Error when it fails and nothing cached`() = runTest {
        coEvery { nominatimRepository.searchVenues(any()) } returns
            ApiResult.Error(ApiErrorType.Type.TIMEOUT)
        val viewModel = viewModel()
        viewModel.search()
        advanceUntilIdle()
        assertEquals(CreateVenueUiState.Error(ApiErrorType.Type.TIMEOUT), viewModel.uiState)
    }

    @Test
    fun `cached venues stay in the list when the online search errors offline`() = runTest {
        every { venuesRepository.searchVenues(any()) } returns flowOf(listOf(venue))
        coEvery { nominatimRepository.searchVenues(any()) } returns
            ApiResult.Error(ApiErrorType.Type.NETWORK)
        val viewModel = viewModel()
        viewModel.search()
        advanceUntilIdle()
        val state = viewModel.uiState as CreateVenueUiState.Results
        assertEquals(listOf("v1"), state.rows.filterIsInstance<VenueSearchResult.Local>().map { it.venue.id })
    }

    @Test
    fun `selecting a cached venue returns it without creating`() = runTest {
        every { venuesRepository.searchVenues(any()) } returns flowOf(listOf(venue))
        val viewModel = viewModel()
        advanceUntilIdle()

        var saved: Venue? = null
        viewModel.selectVenue(venue) { saved = it }

        assertEquals(venue, saved)
        coVerify(exactly = 0) { venuesRepository.findOrCreateVenue(any()) }
    }

    @Test
    fun `selectVenueFromApi invokes onSaved on Success and toggles isSaving`() = runTest {
        coEvery { venuesRepository.findOrCreateVenue(any()) } returns ApiResult.Success(venue)
        val viewModel = viewModel()

        var saved: Venue? = null
        viewModel.selectVenueFromApi(result) { saved = it }
        advanceUntilIdle()

        assertEquals(venue, saved)
        assertFalse(viewModel.isSaving)
        assertNull(viewModel.saveError)
        coVerify(exactly = 1) { venuesRepository.findOrCreateVenue(any()) }
    }

    @Test
    fun `selectVenueFromApi sets saveError on Error`() = runTest {
        coEvery { venuesRepository.findOrCreateVenue(any()) } returns
            ApiResult.Error(ApiErrorType.Type.CONFLICT)
        val viewModel = viewModel()

        var saved: Venue? = null
        viewModel.selectVenueFromApi(result) { saved = it }
        advanceUntilIdle()

        assertNull(saved)
        assertFalse(viewModel.isSaving)
        assertNotNull(viewModel.saveError)
    }
}
