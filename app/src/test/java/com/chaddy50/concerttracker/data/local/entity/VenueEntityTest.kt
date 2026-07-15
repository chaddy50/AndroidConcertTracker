package com.chaddy50.concerttracker.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VenueEntityTest {

    @Test
    fun `toDomain maps a custom entity with null osm preserving new fields`() {
        val entity = VenueEntity(
            id = "v1",
            name = "Blue Note",
            osmId = null,
            osmType = null,
            address = "131 W 3rd St",
            city = "New York",
            country = "USA",
            website = "https://bluenote.com"
        )

        val venue = entity.toDomain()

        assertEquals("v1", venue.id)
        assertNull(venue.osmId)
        assertNull(venue.osmType)
        assertEquals("131 W 3rd St", venue.address)
        assertEquals("New York", venue.city)
        assertEquals("USA", venue.country)
        assertEquals("https://bluenote.com", venue.website)
    }

    @Test
    fun `toDomain maps a fully populated osm entity`() {
        val entity = VenueEntity("v2", "Symphony Hall", "123", "way", "Boston MA", "Boston", "USA", null)

        val venue = entity.toDomain()

        assertEquals("123", venue.osmId)
        assertEquals("way", venue.osmType)
        assertEquals("Boston", venue.city)
        assertNull(venue.website)
    }

    @Test
    fun `toDomain leaves null new fields null rather than coercing to empty`() {
        val venue = VenueEntity("v3", "Hall", "5", "node").toDomain()

        assertNull(venue.address)
        assertNull(venue.city)
        assertNull(venue.country)
        assertNull(venue.website)
    }
}
