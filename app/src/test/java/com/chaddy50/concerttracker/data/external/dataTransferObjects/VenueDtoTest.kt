package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.local.entity.toDomain as rowToDomain
import com.chaddy50.concerttracker.testJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VenueDtoTest {

    private val json = testJson()

    private val customDto = VenueDto(
        id = "v1",
        name = "Blue Note",
        osmId = null,
        osmType = null,
        formattedAddress = "131 W 3rd St",
        city = "New York",
        country = "USA",
        websiteUri = "https://bluenote.com"
    )

    private val osmDto = VenueDto(
        id = "v2",
        name = "Symphony Hall",
        osmId = "123",
        osmType = "way",
        formattedAddress = "Symphony Hall, Boston",
        city = "Boston",
        country = "USA",
        websiteUri = null
    )

    @Test
    fun `toRow maps a custom dto with null osm and all new fields`() {
        val row = customDto.toRow()
        assertEquals("v1", row.id)
        assertNull(row.osmId)
        assertNull(row.osmType)
        assertEquals("131 W 3rd St", row.address)
        assertEquals("New York", row.city)
        assertEquals("USA", row.country)
        assertEquals("https://bluenote.com", row.website)
    }

    @Test
    fun `toRow maps a fully populated osm dto`() {
        val row = osmDto.toRow()
        assertEquals("123", row.osmId)
        assertEquals("way", row.osmType)
        assertEquals("Boston", row.city)
        assertNull(row.website)
    }

    @Test
    fun `toDomain maps a custom dto preserving null osm and new fields`() {
        val venue = customDto.toDomain()
        assertNull(venue.osmId)
        assertEquals("New York", venue.city)
        assertEquals("https://bluenote.com", venue.website)
    }

    @Test
    fun `toRow toDomain and toDomain agree field for field`() {
        assertEquals(customDto.toDomain(), customDto.toRow().rowToDomain())
    }

    @Test
    fun `deserializes snake_case json with null osm into a dto`() {
        val body =
            """{"id":"v1","name":"Blue Note","formatted_address":"131 W 3rd St","city":"New York","country":"USA","website_uri":"https://bluenote.com"}"""
        val dto = json.decodeFromString<VenueDto>(body)
        assertNull(dto.osmId)
        assertNull(dto.osmType)
        assertEquals("New York", dto.city)
    }

    @Test
    fun `deserializes json omitting optional new fields as null`() {
        val body = """{"id":"v1","name":"Hall","osm_id":"5","osm_type":"node"}"""
        val dto = json.decodeFromString<VenueDto>(body)
        assertEquals("5", dto.osmId)
        assertNull(dto.formattedAddress)
        assertNull(dto.city)
        assertNull(dto.country)
        assertNull(dto.websiteUri)
    }
}
