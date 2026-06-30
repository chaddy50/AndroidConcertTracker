package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformancesRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var repository: PerformancesRepository

    private fun performanceJson(id: String = "p1", status: String = "UPCOMING") = """
        {"id":"$id","date":"2024-06-01T19:00:00Z","venue":{"id":"v1","name":"Hall","osm_id":"123","osm_type":"way"},"performers":[{"id":"orchestra","name":"Orch","type":"ORCHESTRA","musicbrainz_id":"mb1"}],"conductor":{"id":"maestro","name":"Cond","type":"CONDUCTOR"},"status":"$status","set_list":[{"id":"${id}_s1","work":{"id":"w1","title":"Symphony","composers":[{"id":"c1","name":"Bach","sort_name":"Bach, JS","open_opus_id":"oo1"}]},"order":1,"featured_performers":[{"performer":{"id":"soloist","name":"Pianist","type":"SOLO"},"role":"Piano"}],"notes":"Wow"}]}
    """.trimIndent()

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        db = inMemoryDatabase()
        repository = PerformancesRepository(
            settingsRepository, OkHttpClient(), json, db,
            db.performanceDao(), db.setListEntryDao(), db.venueDao(),
            db.performerDao(), db.workDao(), db.composerDao()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    @Test
    fun `loadPerformances persists performances and exposes them via observe flows`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${performanceJson()}]"))

        val result = repository.loadPerformances()

        assertTrue(result is ApiResult.Success)
        assertEquals(listOf("p1"), repository.observeUpcomingPerformances().first().map { it.id })
    }

    @Test
    fun `loadPerformances round-trips the full nested graph back to a domain model`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${performanceJson()}]"))
        repository.loadPerformances()

        val performance = repository.observePerformance("p1").first()!!

        assertEquals("Hall", performance.venue.name)
        assertEquals("maestro", performance.conductor?.id)
        assertEquals(listOf("orchestra"), performance.performers.map { it.id })
        val entry = performance.setList.single()
        assertEquals("Symphony", entry.work.title)
        assertEquals(listOf("Bach"), entry.work.composers.map { it.name })
        assertEquals("Piano", entry.featuredPerformers.single().role)
        assertEquals("Wow", entry.notes)
    }

    @Test
    fun `loadPerformances returns Error and preserves cache on server failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${performanceJson()}]"))
        repository.loadPerformances()
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = repository.loadPerformances()

        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), result)
        assertEquals(listOf("p1"), repository.observeUpcomingPerformances().first().map { it.id })
    }

    @Test
    fun `loadPerformances removes performances that no longer exist on the server`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("[${performanceJson("p1")},${performanceJson("p2")}]")
        )
        repository.loadPerformances()
        assertEquals(2, repository.observeUpcomingPerformances().first().size)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${performanceJson("p1")}]"))
        repository.loadPerformances()

        assertEquals(listOf("p1"), repository.observeUpcomingPerformances().first().map { it.id })
    }

    @Test
    fun `createPerformance persists the created performance into the cache`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(performanceJson()))

        val result = repository.createPerformance(
            PerformanceRequest("2024-06-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        )

        assertTrue(result is ApiResult.Success)
        assertEquals("p1", repository.observePerformance("p1").first()?.id)
    }

    @Test
    fun `createPerformance returns Error and leaves cache untouched on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = repository.createPerformance(
            PerformanceRequest("2024-06-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        )

        assertTrue(result is ApiResult.Error)
        assertNull(repository.observePerformance("p1").first())
    }

    @Test
    fun `updatePerformance writes the updated performance through to the cache`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceJson(status = "ATTENDED")))

        val result = repository.updatePerformance(
            "p1",
            PerformanceRequest("2024-06-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.ATTENDED)
        )

        assertTrue(result is ApiResult.Success)
        assertEquals(PerformanceStatus.ATTENDED, repository.observePerformance("p1").first()?.status)
    }

    @Test
    fun `deletePerformance removes the performance from the cache`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${performanceJson()}]"))
        repository.loadPerformances()
        mockWebServer.enqueue(MockResponse().setResponseCode(204))

        val result = repository.deletePerformance("p1")

        assertTrue(result is ApiResult.Success)
        assertTrue(repository.observeUpcomingPerformances().first().isEmpty())
    }

    @Test
    fun `getPerformance returns Success and caches it`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceJson()))

        val result = repository.getPerformance("p1")

        assertTrue(result is ApiResult.Success)
        assertEquals("p1", (result as ApiResult.Success).data.id)
        assertEquals("p1", repository.observePerformance("p1").first()?.id)
    }

    @Test
    fun `getPerformance returns Error on 404`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), repository.getPerformance("p1"))
    }
}
