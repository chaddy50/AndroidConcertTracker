package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VenueSearchTest {

    @Test
    fun `pendingVenueFlow emits null when neither key is set`() = runTest {
        assertNull(SavedStateHandle().pendingVenueFlow().first())
    }

    @Test
    fun `pendingVenueFlow emits null when only the id is set`() = runTest {
        val handle = SavedStateHandle()
        handle["selectedVenueId"] = "v1"
        assertNull(handle.pendingVenueFlow().first())
    }

    @Test
    fun `pendingVenueFlow emits null when only the name is set`() = runTest {
        val handle = SavedStateHandle()
        handle["selectedVenueName"] = "Blue Note"
        assertNull(handle.pendingVenueFlow().first())
    }

    @Test
    fun `pendingVenueFlow emits a result once both keys are set`() = runTest {
        val handle = SavedStateHandle()
        handle["selectedVenueId"] = "v1"
        handle["selectedVenueName"] = "Blue Note"

        assertEquals(PendingVenueResult("v1", "Blue Note"), handle.pendingVenueFlow().first())
    }

    @Test
    fun `clearPendingVenue resets both keys so the flow re-emits null`() = runTest {
        val handle = SavedStateHandle()
        handle["selectedVenueId"] = "v1"
        handle["selectedVenueName"] = "Blue Note"
        handle.clearPendingVenue()

        assertNull(handle.pendingVenueFlow().first())
    }
}
