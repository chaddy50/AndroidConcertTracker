package com.chaddy50.concerttracker.ui.screens.createCustomVenueScreen

import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.VenueRequest
import com.chaddy50.concerttracker.data.repository.VenuesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CreateCustomVenueViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val venuesRepository: VenuesRepository = mockk()
    private val venue = Venue("v1", "Blue Note", null, null, address = "131 W 3rd St")

    private fun viewModel() = CreateCustomVenueViewModel(venuesRepository)

    /** Fills every required field (name/address/city/country) so a save passes validation. */
    private fun CreateCustomVenueViewModel.fillRequired() {
        updateName("Blue Note")
        updateAddress("131 W 3rd St")
        updateCity("New York")
        updateCountry("USA")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no field errors are shown before a save attempt`() {
        val vm = viewModel()
        assertFalse(vm.nameError)
        assertFalse(vm.addressError)
        assertFalse(vm.cityError)
        assertFalse(vm.countryError)
    }

    @Test
    fun `save with all required fields blank flags every required field and skips the repository`() = runTest {
        val vm = viewModel()

        vm.save {}
        advanceUntilIdle()

        assertTrue(vm.nameError)
        assertTrue(vm.addressError)
        assertTrue(vm.cityError)
        assertTrue(vm.countryError)
        coVerify(exactly = 0) { venuesRepository.findOrCreateVenue(any()) }
    }

    @Test
    fun `save flags only the missing required fields`() = runTest {
        val vm = viewModel()
        vm.updateName("Blue Note")
        vm.updateCity("New York")

        vm.save {}
        advanceUntilIdle()

        assertFalse(vm.nameError)
        assertFalse(vm.cityError)
        assertTrue(vm.addressError)
        assertTrue(vm.countryError)
        coVerify(exactly = 0) { venuesRepository.findOrCreateVenue(any()) }
    }

    @Test
    fun `a whitespace-only required field still fails validation`() = runTest {
        val vm = viewModel()
        vm.fillRequired()
        vm.updateCity("   ")

        vm.save {}
        advanceUntilIdle()

        assertTrue(vm.cityError)
        coVerify(exactly = 0) { venuesRepository.findOrCreateVenue(any()) }
    }

    @Test
    fun `filling a flagged field clears its error`() = runTest {
        val vm = viewModel()
        vm.save {}
        advanceUntilIdle()
        assertTrue(vm.addressError)

        vm.updateAddress("131 W 3rd St")

        assertFalse(vm.addressError)
    }

    @Test
    fun `field updates set their backing state`() {
        val vm = viewModel()
        vm.updateName("A")
        vm.updateAddress("B")
        vm.updateCity("C")
        vm.updateCountry("D")
        vm.updateWebsite("E")
        assertEquals("A", vm.name)
        assertEquals("B", vm.address)
        assertEquals("C", vm.city)
        assertEquals("D", vm.country)
        assertEquals("E", vm.website)
    }

    @Test
    fun `save invokes callback on success and clears loading`() = runTest {
        coEvery { venuesRepository.findOrCreateVenue(any()) } returns ApiResult.Success(venue)
        val vm = viewModel()
        vm.fillRequired()

        var saved: Venue? = null
        vm.save { saved = it }
        advanceUntilIdle()

        assertEquals(venue, saved)
        assertFalse(vm.isSaving)
        assertNull(vm.saveError)
    }

    @Test
    fun `save sets saveError and does not call back on error`() = runTest {
        coEvery { venuesRepository.findOrCreateVenue(any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER)
        val vm = viewModel()
        vm.fillRequired()

        var saved: Venue? = null
        vm.save { saved = it }
        advanceUntilIdle()

        assertNull(saved)
        assertFalse(vm.isSaving)
        assertNotNull(vm.saveError)
    }

    @Test
    fun `isSaving is true while the call is in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { venuesRepository.findOrCreateVenue(any()) } coAnswers {
            gate.await()
            ApiResult.Success(venue)
        }
        val vm = viewModel()
        vm.fillRequired()

        vm.save {}
        advanceUntilIdle() // runs the coroutine up to the awaiting repository call
        assertTrue(vm.isSaving)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.isSaving)
    }

    @Test
    fun `save builds a request with null osm and the entered fields`() = runTest {
        val request = slot<VenueRequest>()
        coEvery { venuesRepository.findOrCreateVenue(capture(request)) } returns ApiResult.Success(venue)
        val vm = viewModel()
        vm.updateName("Blue Note")
        vm.updateAddress("131 W 3rd St")
        vm.updateCity("New York")
        vm.updateCountry("USA")
        vm.updateWebsite("https://bluenote.com")

        vm.save {}
        advanceUntilIdle()

        assertNull(request.captured.osmType)
        assertNull(request.captured.osmId)
        assertEquals("Blue Note", request.captured.name)
        assertEquals("131 W 3rd St", request.captured.formattedAddress)
        assertEquals("New York", request.captured.city)
        assertEquals("USA", request.captured.country)
        assertEquals("https://bluenote.com", request.captured.websiteUri)
    }

    @Test
    fun `save trims required fields and sends a blank website as null`() = runTest {
        val request = slot<VenueRequest>()
        coEvery { venuesRepository.findOrCreateVenue(capture(request)) } returns ApiResult.Success(venue)
        val vm = viewModel()
        vm.updateName("  Blue Note  ")
        vm.updateAddress("  131 W 3rd St  ")
        vm.updateCity("New York")
        vm.updateCountry("USA")

        vm.save {}
        advanceUntilIdle()

        assertEquals("Blue Note", request.captured.name)
        assertEquals("131 W 3rd St", request.captured.formattedAddress)
        assertNull(request.captured.websiteUri)
    }

    @Test
    fun `save clears a prior error before retrying`() = runTest {
        coEvery { venuesRepository.findOrCreateVenue(any()) } returns
            ApiResult.Error(ApiErrorType.Type.SERVER) andThen ApiResult.Success(venue)
        val vm = viewModel()
        vm.fillRequired()

        vm.save {}
        advanceUntilIdle()
        assertNotNull(vm.saveError)

        vm.save {}
        advanceUntilIdle()
        assertNull(vm.saveError)
    }
}
