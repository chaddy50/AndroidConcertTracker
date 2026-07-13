package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import com.chaddy50.concerttracker.data.local.syncOperationsRepository
import com.chaddy50.concerttracker.data.sync.SyncScheduler
import com.chaddy50.concerttracker.testJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.inject.Provider
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
    private val syncScheduler: SyncScheduler = mockk(relaxed = true)
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var repository: PerformancesRepository

    private fun performanceJson(id: String = "p1", status: String = "UPCOMING") = """
        {"id":"$id","date":"2099-06-01T19:00:00Z","venue":{"id":"v1","name":"Hall","osm_id":"123","osm_type":"way"},"performers":[{"id":"orchestra","name":"Orch","type":"ORCHESTRA","musicbrainz_id":"mb1"}],"conductor":{"id":"maestro","name":"Cond","type":"CONDUCTOR"},"status":"$status","set_list":[{"id":"${id}_s1","work":{"id":"w1","title":"Symphony","composers":[{"id":"c1","name":"Bach","sort_name":"Bach, JS","open_opus_id":"oo1"}]},"order":1,"featured_performers":[{"performer":{"id":"soloist","name":"Pianist","type":"SOLO"},"role":"Piano"}],"notes":"Wow"}]}
    """.trimIndent()

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        db = inMemoryDatabase()
        repository = PerformancesRepository(
            settingsRepository, OkHttpClient(), json, db,
            db.performanceDao(), db.setListEntryDao(), db.venueDao(),
            db.performerDao(), db.workDao(), db.composerDao(),
            db.syncOperationsRepository(), Provider { syncScheduler }
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

    /** Seed a synced performance + its cached venue into Room (via the online pull). */
    private suspend fun seedSyncedPerformance() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${performanceJson()}]"))
        repository.loadPerformances()
    }

    @Test
    fun `createPerformance persists a PENDING performance locally and enqueues a CREATE op with no network call`() = runTest {
        seedSyncedPerformance() // caches venue v1
        val requestsBefore = mockWebServer.requestCount

        val result = repository.createPerformance(
            PerformanceRequest("2024-07-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        )

        assertTrue(result is ApiResult.Success)
        val created = (result as ApiResult.Success).data
        assertEquals(created.id, repository.observePerformance(created.id).first()?.id)
        assertEquals(com.chaddy50.concerttracker.data.enum.SyncState.PENDING, created.syncState) // domain flag
        assertEquals("PENDING", db.performanceDao().getById(created.id)?.syncState)
        assertEquals(requestsBefore, mockWebServer.requestCount) // no network on the write path

        val op = db.syncOperationDao().getAllOrdered().single { it.entityId == created.id }
        assertEquals("PERFORMANCE", op.entityType)
        assertEquals("CREATE", op.operationType)
        assertTrue(op.payloadJson!!.contains(created.id))
        verify { syncScheduler.requestSync() }
    }

    @Test
    fun `two offline creates yield two distinct ids, rows, and ops`() = runTest {
        seedSyncedPerformance()
        val request = PerformanceRequest("2024-07-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)

        val a = (repository.createPerformance(request) as ApiResult.Success).data
        val b = (repository.createPerformance(request) as ApiResult.Success).data

        assertTrue(a.id != b.id)
        assertEquals(2, db.syncOperationDao().getAllOrdered().count { it.operationType == "CREATE" })
    }

    @Test
    fun `updatePerformance mutates in place as PENDING and enqueues one UPDATE op`() = runTest {
        seedSyncedPerformance()
        val requestsBefore = mockWebServer.requestCount

        val result = repository.updatePerformance(
            "p1",
            PerformanceRequest("2024-06-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.ATTENDED)
        )

        assertTrue(result is ApiResult.Success)
        assertEquals(PerformanceStatus.ATTENDED, repository.observePerformance("p1").first()?.status)
        assertEquals("PENDING", db.performanceDao().getById("p1")?.syncState)
        assertEquals(requestsBefore, mockWebServer.requestCount)
        val ops = db.syncOperationDao().getAllOrdered().filter { it.entityId == "p1" }
        assertEquals(listOf("UPDATE"), ops.map { it.operationType })
    }

    @Test
    fun `deletePerformance of a synced row tombstones it and enqueues a DELETE op`() = runTest {
        seedSyncedPerformance()
        val requestsBefore = mockWebServer.requestCount

        val result = repository.deletePerformance("p1")

        assertTrue(result is ApiResult.Success)
        assertTrue(repository.observeUpcomingPerformances().first().isEmpty()) // hidden as PENDING_DELETE
        assertEquals("PENDING_DELETE", db.performanceDao().getById("p1")?.syncState)
        assertEquals(requestsBefore, mockWebServer.requestCount)
        val op = db.syncOperationDao().getAllOrdered().single { it.entityId == "p1" }
        assertEquals("DELETE", op.operationType)
        assertNull(op.payloadJson)
    }

    @Test
    fun `deletePerformance of an unsynced row hard-deletes it and drops its queued CREATE op`() = runTest {
        seedSyncedPerformance() // caches venue v1
        val created = (repository.createPerformance(
            PerformanceRequest("2024-07-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        ) as ApiResult.Success).data

        repository.deletePerformance(created.id)

        assertNull(db.performanceDao().getById(created.id))
        assertTrue(db.syncOperationDao().getAllOrdered().none { it.entityId == created.id })
    }

    @Test
    fun `getPerformance returns the cached performance without a network call`() = runTest {
        seedSyncedPerformance()
        val requestsBefore = mockWebServer.requestCount

        val performance = repository.getPerformance("p1")

        assertEquals("p1", performance?.id)
        // Reads straight from Room (the single source of truth) — no network round-trip.
        assertEquals(requestsBefore, mockWebServer.requestCount)
    }

    @Test
    fun `getPerformance returns the fully hydrated graph from the cache`() = runTest {
        seedSyncedPerformance()

        val performance = repository.getPerformance("p1")!!

        assertEquals("Hall", performance.venue.name)
        assertEquals("maestro", performance.conductor?.id)
        assertEquals(listOf("orchestra"), performance.performers.map { it.id })
        val entry = performance.setList.single()
        assertEquals("Symphony", entry.work.title)
        assertEquals("Piano", entry.featuredPerformers.single().role)
        assertEquals("Wow", entry.notes)
    }

    @Test
    fun `getPerformance returns a locally-created PENDING performance offline`() = runTest {
        seedSyncedPerformance() // caches venue v1
        val created = (repository.createPerformance(
            PerformanceRequest("2024-07-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        ) as ApiResult.Success).data

        assertEquals(created.id, repository.getPerformance(created.id)?.id)
    }

    @Test
    fun `getPerformance returns null when the performance is not cached`() = runTest {
        assertNull(repository.getPerformance("missing"))
    }
}
