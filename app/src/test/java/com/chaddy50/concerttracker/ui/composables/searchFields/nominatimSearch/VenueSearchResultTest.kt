package com.chaddy50.concerttracker.ui.composables.searchFields.nominatimSearch

import com.chaddy50.concerttracker.data.domain.Venue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VenueSearchResultTest {

    @Test
    fun `Local address returns the venue address when present`() {
        val venue = Venue("v1", "Blue Note", null, null, address = "131 W 3rd St")
        assertEquals("131 W 3rd St", VenueSearchResult.Local(venue).address)
    }

    @Test
    fun `Local address is null when the venue has no address`() {
        val venue = Venue("v1", "Hall", "123", "way")
        assertNull(VenueSearchResult.Local(venue).address)
    }

    @Test
    fun `Local name returns the venue name`() {
        val venue = Venue("v1", "Blue Note", null, null, address = "131 W 3rd St")
        assertEquals("Blue Note", VenueSearchResult.Local(venue).name)
    }
}
