package com.chaddy50.concerttracker.data.external.api

import com.chaddy50.concerttracker.testJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NominatimApiServiceTest {

    private val json = testJson()

    @Test
    fun `serializes a custom VenueRequest with null osm and new fields`() {
        val body = json.encodeToString(
            VenueRequest(name = "Blue Note", formattedAddress = "131 W 3rd St", city = "New York", country = "USA")
        )
        assertTrue(body.contains("\"name\":\"Blue Note\""))
        assertTrue(body.contains("\"formatted_address\":\"131 W 3rd St\""))
        assertTrue(body.contains("\"city\":\"New York\""))
        // Null-defaulted osm fields are omitted (encodeDefaults is off, mirroring production Json).
        assertFalse(body.contains("osm_type"))
        assertFalse(body.contains("osm_id"))
    }

    @Test
    fun `serializes an OSM VenueRequest with address details`() {
        val body = json.encodeToString(
            VenueRequest(osmType = "way", osmId = "9", name = "Hall", formattedAddress = "Hall, City", city = "City")
        )
        assertTrue(body.contains("\"osm_type\":\"way\""))
        assertTrue(body.contains("\"osm_id\":\"9\""))
        assertTrue(body.contains("\"city\":\"City\""))
    }

    @Test
    fun `deserializes a Nominatim result with a nested address object`() {
        val body =
            """{"osm_id":42,"osm_type":"way","display_name":"Hall, City","name":"Hall","address":{"city":"Boston","country":"USA"}}"""
        val result = json.decodeFromString<NominatimResult>(body)
        assertEquals("Boston", result.address?.city)
        assertEquals("USA", result.address?.country)
    }

    @Test
    fun `deserializes a Nominatim result without an address object as null`() {
        val body = """{"osm_id":42,"osm_type":"way","display_name":"Hall, City","name":"Hall"}"""
        val result = json.decodeFromString<NominatimResult>(body)
        assertNull(result.address)
    }
}
