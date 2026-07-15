package com.chaddy50.concerttracker.data.local.dao

import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VenueDaoTest {

    private lateinit var db: ConcertTrackerDatabase
    private lateinit var dao: VenueDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.venueDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `a venue with all new columns round-trips through the dao`() = runTest {
        dao.upsert(
            listOf(
                VenueEntity(
                    id = "v1",
                    name = "Blue Note",
                    osmId = "123",
                    osmType = "way",
                    address = "131 W 3rd St",
                    city = "New York",
                    country = "USA",
                    website = "https://bluenote.com"
                )
            )
        )

        val cached = dao.getById("v1")!!
        assertEquals("131 W 3rd St", cached.address)
        assertEquals("New York", cached.city)
        assertEquals("USA", cached.country)
        assertEquals("https://bluenote.com", cached.website)
    }

    @Test
    fun `a custom venue with null osm columns inserts and reads back as null`() = runTest {
        dao.upsert(listOf(VenueEntity("v2", "Custom Venue", null, null, address = "Somewhere")))

        val cached = dao.getById("v2")!!
        assertNull(cached.osmId)
        assertNull(cached.osmType)
        assertEquals("Somewhere", cached.address)
    }
}
