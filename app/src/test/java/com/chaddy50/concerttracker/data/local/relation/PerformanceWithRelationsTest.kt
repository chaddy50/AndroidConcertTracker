package com.chaddy50.concerttracker.data.local.relation

import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.local.entity.PerformanceEntity
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceWithRelationsTest {

    private fun withNotes(notes: String) = PerformanceWithRelations(
        performance = PerformanceEntity(
            id = "p1",
            date = "2024-06-01T19:00:00Z",
            status = PerformanceStatus.ATTENDED.name,
            venueId = "v1",
            notes = notes
        ),
        venue = VenueEntity("v1", "Hall", "osm", "way"),
        performers = emptyList(),
        conductor = null,
        setList = emptyList()
    )

    @Test
    fun `toDomain maps notes onto the domain performance`() {
        assertEquals("Loved it", withNotes("Loved it").toDomain().notes)
    }

    @Test
    fun `toDomain maps an empty notes to empty`() {
        assertEquals("", withNotes("").toDomain().notes)
    }
}
