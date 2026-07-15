package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.VenueRequest
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import com.chaddy50.concerttracker.testJson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VenuesRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase

    private lateinit var venuesRepository: VenuesRepository

    private val venueJson = """{"id":"v1","name":"Test Hall","osm_id":"123","osm_type":"way"}"""
    private val customVenueJson =
        """{"id":"v2","name":"Blue Note","formatted_address":"131 W 3rd St","city":"New York","country":"USA","website_uri":"https://bluenote.com"}"""
    private val osmWithAddressJson =
        """{"id":"v3","name":"Hall","osm_id":"9","osm_type":"way","formatted_address":"Hall, City","city":"City","country":"Country"}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        val client = OkHttpClient()
        db = inMemoryDatabase()
        venuesRepository = VenuesRepository(settingsRepository, client, json, db.venueDao())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    @Test
    fun `findOrCreateVenue returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(venueJson))
        assertTrue(venuesRepository.findOrCreateVenue(VenueRequest("way", "123", "Hall")) is ApiResult.Success)
    }

    @Test
    fun `findOrCreateVenue returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(venuesRepository.findOrCreateVenue(VenueRequest("way", "123", "Hall")) is ApiResult.Error)
    }

    @Test
    fun `findOrCreateVenue writes the created venue through to Room`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(venueJson))

        val result = venuesRepository.findOrCreateVenue(VenueRequest("way", "123", "Hall"))

        assertTrue(result is ApiResult.Success)
        val cached = db.venueDao().getById("v1")
        assertEquals("Test Hall", cached?.name)
        assertEquals("123", cached?.osmId)
        assertEquals("way", cached?.osmType)
        assertEquals(listOf("v1"), venuesRepository.searchVenues("").first().map { it.id })
    }

    @Test
    fun `findOrCreateVenue writes nothing to Room on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        venuesRepository.findOrCreateVenue(VenueRequest("way", "123", "Hall"))

        assertTrue(venuesRepository.searchVenues("").first().isEmpty())
    }

    @Test
    fun `searchVenues emits cached venues mapped to domain`() = runTest {
        db.venueDao().upsert(listOf(VenueEntity("v1", "Symphony Hall", "1", "way")))
        val venues = venuesRepository.searchVenues("").first()
        assertEquals(listOf("v1"), venues.map { it.id })
        assertEquals("Symphony Hall", venues.single().name)
    }

    @Test
    fun `searchVenues filters by case-insensitive name substring`() = runTest {
        db.venueDao().upsert(
            listOf(
                VenueEntity("v1", "Symphony Hall", "1", "way"),
                VenueEntity("v2", "Opera House", "2", "way")
            )
        )
        assertEquals(listOf("v1"), venuesRepository.searchVenues("SYM").first().map { it.id })
    }

    @Test
    fun `searchVenues emits empty when nothing cached`() = runTest {
        assertTrue(venuesRepository.searchVenues("").first().isEmpty())
    }

    @Test
    fun `findOrCreateVenue persists a custom venue with null osm identity and new fields`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(customVenueJson))

        val result = venuesRepository.findOrCreateVenue(
            VenueRequest(
                name = "Blue Note",
                formattedAddress = "131 W 3rd St",
                city = "New York",
                country = "USA",
                websiteUri = "https://bluenote.com"
            )
        )

        assertTrue(result is ApiResult.Success)
        val cached = db.venueDao().getById("v2")!!
        assertNull(cached.osmId)
        assertNull(cached.osmType)
        assertEquals("131 W 3rd St", cached.address)
        assertEquals("New York", cached.city)
        assertEquals("USA", cached.country)
        assertEquals("https://bluenote.com", cached.website)
    }

    @Test
    fun `findOrCreateVenue returns a custom venue domain model with null osm and new fields`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(customVenueJson))

        val result = venuesRepository.findOrCreateVenue(VenueRequest(name = "Blue Note", city = "New York"))

        val venue = (result as ApiResult.Success).data
        assertNull(venue.osmId)
        assertNull(venue.osmType)
        assertEquals("New York", venue.city)
        assertEquals("131 W 3rd St", venue.address)
    }

    @Test
    fun `findOrCreateVenue persists address city and country for an OSM venue`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(osmWithAddressJson))

        venuesRepository.findOrCreateVenue(
            VenueRequest(osmType = "way", osmId = "9", name = "Hall", formattedAddress = "Hall, City", city = "City", country = "Country")
        )

        val cached = db.venueDao().getById("v3")!!
        assertEquals("9", cached.osmId)
        assertEquals("Hall, City", cached.address)
        assertEquals("City", cached.city)
        assertEquals("Country", cached.country)
        assertNull(cached.website)
    }

    @Test
    fun `findOrCreateVenue stores nulls when an OSM response omits the new fields`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(201)
                .setBody("""{"id":"v4","name":"Hall","osm_id":"5","osm_type":"node"}""")
        )

        venuesRepository.findOrCreateVenue(VenueRequest(osmType = "node", osmId = "5", name = "Hall"))

        val cached = db.venueDao().getById("v4")!!
        assertEquals("5", cached.osmId)
        assertNull(cached.address)
        assertNull(cached.city)
        assertNull(cached.country)
        assertNull(cached.website)
    }

    @Test
    fun `findOrCreateVenue sends the custom fields and omits null osm in the request body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(customVenueJson))

        venuesRepository.findOrCreateVenue(VenueRequest(name = "Blue Note", city = "New York"))

        val body = mockWebServer.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"name\":\"Blue Note\""))
        assertTrue(body.contains("\"city\":\"New York\""))
        assertFalse(body.contains("osm_type"))
        assertFalse(body.contains("osm_id"))
    }

    @Test
    fun `findOrCreateVenue upsert replaces an existing venue with the latest fields`() = runTest {
        db.venueDao().upsert(listOf(VenueEntity("v2", "Old Name", "1", "way", address = "old addr")))
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(customVenueJson))

        venuesRepository.findOrCreateVenue(VenueRequest(name = "Blue Note"))

        val cached = db.venueDao().getById("v2")!!
        assertEquals("Blue Note", cached.name)
        assertEquals("131 W 3rd St", cached.address)
        assertNull(cached.osmId)
    }

    @Test
    fun `searchVenues emits a cached custom venue with null osm mapped to domain`() = runTest {
        db.venueDao().upsert(listOf(VenueEntity("v2", "Blue Note", null, null, address = "131 W 3rd St")))

        val venue = venuesRepository.searchVenues("").first().single()

        assertEquals("v2", venue.id)
        assertNull(venue.osmId)
        assertEquals("131 W 3rd St", venue.address)
    }
}
