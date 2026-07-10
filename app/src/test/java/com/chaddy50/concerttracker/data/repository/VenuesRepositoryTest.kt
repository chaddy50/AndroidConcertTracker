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
}
