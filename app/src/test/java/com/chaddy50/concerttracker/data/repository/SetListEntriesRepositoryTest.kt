package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryRequest
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import com.chaddy50.concerttracker.testJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
class SetListEntriesRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var performancesRepository: PerformancesRepository
    private lateinit var repository: SetListEntriesRepository

    private val entryResponseJson =
        """{"id":"p1_s1","work":{"id":"w1","title":"Symphony","composers":[]},"order":1,"featured_performers":[]}"""

    private fun parentJson(notes: String?) = """
        {"id":"p1","date":"2024-06-01T19:00:00Z","venue":{"id":"v1","name":"Hall","osm_id":"1","osm_type":"way"},"performers":[],"status":"UPCOMING","set_list":[{"id":"p1_s1","work":{"id":"w1","title":"Symphony","composers":[]},"order":1,"featured_performers":[],"notes":${if (notes == null) "null" else "\"$notes\""}}]}
    """.trimIndent()

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        db = inMemoryDatabase()
        performancesRepository = PerformancesRepository(
            settingsRepository, OkHttpClient(), json, db,
            db.performanceDao(), db.setListEntryDao(), db.venueDao(),
            db.performerDao(), db.workDao(), db.composerDao()
        )
        repository = SetListEntriesRepository(
            settingsRepository, OkHttpClient(), json, db.setListEntryDao(), performancesRepository
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    @Test
    fun `createSetListEntry succeeds and re-fetches the parent into the cache`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(entryResponseJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(parentJson(notes = null)))

        val result = repository.createSetListEntry(SetListEntryCreateRequest("p1", "w1", 1, emptyList()))

        assertTrue(result is ApiResult.Success)
        val cached = performancesRepository.observePerformance("p1").first()
        assertEquals(listOf("p1_s1"), cached?.setList?.map { it.id })
    }

    @Test
    fun `createSetListEntry returns Error and does not touch the cache on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = repository.createSetListEntry(SetListEntryCreateRequest("p1", "w1", 1, emptyList()))

        assertTrue(result is ApiResult.Error)
        assertEquals(null, performancesRepository.observePerformance("p1").first())
    }

    @Test
    fun `updateSetListEntry writes the refreshed notes through to the cache`() = runTest {
        // seed the cache so getPerformanceIdFor can resolve the parent
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${parentJson(notes = null)}]"))
        performancesRepository.loadPerformances()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(entryResponseJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(parentJson(notes = "Great")))

        val result = repository.updateSetListEntry("p1_s1", SetListEntryRequest(notes = "Great"))

        assertTrue(result is ApiResult.Success)
        val cached = performancesRepository.observePerformance("p1").first()
        assertEquals("Great", cached?.setList?.single()?.notes)
    }

    @Test
    fun `deleteSetListEntry removes the entry from the cached parent`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${parentJson(notes = null)}]"))
        performancesRepository.loadPerformances()

        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        // parent re-fetch now returns no set-list entries
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"p1","date":"2024-06-01T19:00:00Z","venue":{"id":"v1","name":"Hall","osm_id":"1","osm_type":"way"},"performers":[],"status":"UPCOMING","set_list":[]}"""
            )
        )

        val result = repository.deleteSetListEntry("p1_s1")

        assertTrue(result is ApiResult.Success)
        val cached = performancesRepository.observePerformance("p1").first()
        assertTrue(cached?.setList?.isEmpty() == true)
    }
}
