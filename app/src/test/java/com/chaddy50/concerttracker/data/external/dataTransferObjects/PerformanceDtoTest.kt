package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerformanceDtoTest {

    @Test
    fun `toRows maps scalar fields and venue FK`() {
        val graph = Fixtures.performance(id = "p1").toRows()

        assertEquals("p1", graph.performance.id)
        assertEquals("v1", graph.performance.venueId)
        assertNull(graph.performance.conductorId)
        assertEquals(1, graph.venues.size)
    }

    @Test
    fun `toRows emits one performer cross-ref per headline performer`() {
        val graph = Fixtures.performance(
            performers = listOf(Fixtures.performer("a"), Fixtures.performer("b"))
        ).toRows()

        assertEquals(2, graph.headlinePerformers.size)
        assertEquals(setOf("a", "b"), graph.headlinePerformers.map { it.performerId }.toSet())
    }

    @Test
    fun `toRows sets conductorId and includes conductor among performers`() {
        val conductor = Fixtures.performer(id = "maestro")
        val graph = Fixtures.performance(conductor = conductor).toRows()

        assertEquals("maestro", graph.performance.conductorId)
        assertEquals(true, graph.performers.any { it.id == "maestro" })
    }

    @Test
    fun `toRows flattens set list with works, composers and featured performers`() {
        val graph = Fixtures.fullPerformance(id = "p1").toRows()

        assertEquals(2, graph.setListEntries.size)
        assertEquals(setOf("w1", "w2"), graph.works.map { it.id }.toSet())
        // featured performer role preserved
        val pianoRef = graph.featuredPerformers.single { it.performerId == "soloist" }
        assertEquals("Piano", pianoRef.role)
        assertEquals("p1_s1", pianoRef.setListEntryId)
    }

    @Test
    fun `toRows deduplicates shared composers and performers`() {
        val graph = Fixtures.fullPerformance(id = "p1").toRows()

        // "shared" composer is referenced by both works but stored once
        assertEquals(1, graph.composers.count { it.id == "shared" })
        // two work-composer links for the shared composer (one per work)
        assertEquals(2, graph.workComposers.count { it.composerId == "shared" })
    }

    @Test
    fun `toRows maps order and notes onto set list entries`() {
        val graph = Fixtures.fullPerformance(id = "p1").toRows()

        val first = graph.setListEntries.single { it.id == "p1_s1" }
        assertEquals(1, first.order)
        assertEquals("Brilliant", first.notes)
        val second = graph.setListEntries.single { it.id == "p1_s2" }
        assertEquals("", second.notes)
    }

    @Test
    fun `toRows maps performance-level notes onto the performance row, coercing null to empty`() {
        assertEquals("Season opener", Fixtures.performance(notes = "Season opener").toRows().performance.notes)
        assertEquals("", Fixtures.performance(notes = null).toRows().performance.notes)
    }

    @Test
    fun `toDomain maps performance-level notes, coercing null to empty`() {
        assertEquals("Season opener", Fixtures.performance(notes = "Season opener").toDomain().notes)
        assertEquals("", Fixtures.performance(notes = null).toDomain().notes)
    }

    @Test
    fun `toRows handles minimal performance with empty set list and performers`() {
        val graph = Fixtures.performance(performers = emptyList(), setList = emptyList()).toRows()

        assertEquals(0, graph.headlinePerformers.size)
        assertEquals(0, graph.setListEntries.size)
        assertEquals(0, graph.featuredPerformers.size)
        assertEquals(0, graph.works.size)
    }
}
