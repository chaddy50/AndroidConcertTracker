package com.chaddy50.concerttracker.data.sync

import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
import com.chaddy50.concerttracker.data.external.api.PerformerRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryUpdateRequest
import com.chaddy50.concerttracker.data.external.api.WorkRequest
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.SyncOperationEntity
import com.chaddy50.concerttracker.data.local.entity.PerformanceEntity
import com.chaddy50.concerttracker.data.local.entity.VenueEntity
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import com.chaddy50.concerttracker.data.local.syncOperationsRepository
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import com.chaddy50.concerttracker.data.repository.SyncOperationsRepository
import com.chaddy50.concerttracker.testJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
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
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncManagerTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var performancesRepository: PerformancesRepository
    private lateinit var setListEntriesRepository: SetListEntriesRepository
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        db = inMemoryDatabase()
        val syncOperationsRepository = db.syncOperationsRepository()
        performancesRepository = PerformancesRepository(
            settingsRepository, OkHttpClient(), json, db,
            db.performanceDao(), db.setListEntryDao(), db.venueDao(),
            db.performerDao(), db.workDao(), db.composerDao(),
            syncOperationsRepository, Provider { mockk(relaxed = true) }
        )
        setListEntriesRepository = SetListEntriesRepository(
            json, db, db.setListEntryDao(), syncOperationsRepository,
            performancesRepository, Provider { mockk(relaxed = true) }
        )
        syncManager = SyncManager(
            settingsRepository, OkHttpClient(), json, syncOperationsRepository,
            performancesRepository, setListEntriesRepository
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    private suspend fun enqueueOp(
        entityType: SyncEntityType,
        syncOperationType: SyncOperationType,
        entityLocalId: String,
        payloadJson: String?
    ) = db.syncOperationDao().enqueue(
        SyncOperationEntity(
            entityType = entityType.name,
            operationType = syncOperationType.name,
            entityId = entityLocalId,
            payloadJson = payloadJson,
            createdAt = "2026-01-01T00:00:00Z"
        )
    )

    private fun reconcileResponse() = MockResponse().setResponseCode(200).setBody("[]")

    @Test
    fun `drains a CREATE, sends the client id, marks synced, and runs the pull reconcile`() = runTest {
        val payload = json.encodeToString(PerformerRequest("Solo", PerformerType.OTHER, id = "perf-1"))
        enqueueOp(SyncEntityType.PERFORMER, SyncOperationType.CREATE, "perf-1", payload)
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"perf-1","name":"Solo","type":"OTHER"}"""))
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish)
        assertEquals(1, result.numberOfOperationsProcessed)
        assertTrue(db.syncOperationDao().getAllOrdered().isEmpty())
        val post = mockWebServer.takeRequest()
        assertEquals("POST", post.method)
        assertTrue("client id transmitted", post.body.readUtf8().contains("perf-1"))
        assertEquals("GET", mockWebServer.takeRequest().method) // pull reconcile
    }

    @Test
    fun `treats a CREATE 409 as success`() = runTest {
        enqueueOp(
            SyncEntityType.PERFORMER, SyncOperationType.CREATE, "perf-1",
            json.encodeToString(PerformerRequest("Solo", PerformerType.OTHER, id = "perf-1"))
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody("""{"detail":"Performer perf-1 already exists"}"""))
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish)
        assertEquals(1, result.numberOfOperationsProcessed)
        assertTrue(db.syncOperationDao().getAllOrdered().isEmpty())
    }

    @Test
    fun `drains a set-list UPDATE via the typed endpoint carrying the snapshot`() = runTest {
        enqueueOp(
            SyncEntityType.SET_LIST_ENTRY, SyncOperationType.UPDATE, "e-1",
            json.encodeToString(
                SetListEntryUpdateRequest(workId = "w1", order = 1, featuredPerformers = emptyList(), notes = "Great")
            )
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"e-1","work":{"id":"w1","title":"W","composers":[]},"order":1,"featured_performers":[],"notes":"Great"}"""
            )
        )
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish)
        val put = mockWebServer.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/v1/set-list-entries/e-1", put.path)
        assertTrue(put.body.readUtf8().contains("Great"))
    }

    @Test
    fun `drains a performer UPDATE via PUT carrying the corrected fields`() = runTest {
        enqueueOp(
            SyncEntityType.PERFORMER, SyncOperationType.UPDATE, "pe-1",
            json.encodeToString(PerformerRequest("Andris Nelsons", PerformerType.CONDUCTOR, "Conductor", id = "pe-1"))
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"pe-1","name":"Andris Nelsons","type":"CONDUCTOR","specialty":"Conductor"}"""
            )
        )
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish)
        assertEquals(1, result.numberOfOperationsProcessed)
        assertTrue(db.syncOperationDao().getAllOrdered().isEmpty())
        val put = mockWebServer.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/v1/performers/pe-1", put.path)
        val body = put.body.readUtf8()
        assertTrue(body.contains("CONDUCTOR"))
        assertTrue(body.contains("Conductor"))
        assertEquals("GET", mockWebServer.takeRequest().method) // pull reconcile
    }

    @Test
    fun `treats a performer UPDATE 409 as success (last-write-wins)`() = runTest {
        enqueueOp(
            SyncEntityType.PERFORMER, SyncOperationType.UPDATE, "pe-1",
            json.encodeToString(PerformerRequest("Name", PerformerType.SOLO, "Cellist", id = "pe-1"))
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(409))
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish)
        assertEquals(1, result.numberOfOperationsProcessed)
        assertTrue(db.syncOperationDao().getAllOrdered().isEmpty())
    }

    @Test
    fun `a transient failure on a performer UPDATE leaves it pending and records an attempt`() = runTest {
        enqueueOp(
            SyncEntityType.PERFORMER, SyncOperationType.UPDATE, "pe-1",
            json.encodeToString(PerformerRequest("Name", PerformerType.SOLO, "Cellist", id = "pe-1"))
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = syncManager.sync()

        assertFalse(result.didFinish)
        val op = db.syncOperationDao().getAllOrdered().single()
        assertEquals("pe-1", op.entityId)
        assertEquals(1, op.attemptCount)
        assertNull(op.lastError)
    }

    @Test
    fun `drains FIFO so a custom work is sent before the performance that depends on it`() = runTest {
        enqueueOp(
            SyncEntityType.WORK, SyncOperationType.CREATE, "w-1",
            json.encodeToString(WorkRequest("My Work", null, emptyList(), id = "w-1"))
        )
        enqueueOp(
            SyncEntityType.PERFORMANCE, SyncOperationType.CREATE, "p-1",
            json.encodeToString(PerformanceRequest("2026-01-01T00:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING, id = "p-1"))
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"w-1","title":"My Work","composers":[]}"""))
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"p-1","date":"2026-01-01T00:00:00Z","venue":{"id":"v1","name":"H","osm_id":"1","osm_type":"way"},"performers":[],"status":"UPCOMING","set_list":[]}"""))
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish)
        assertEquals(2, result.numberOfOperationsProcessed)
        assertEquals("/v1/works/", mockWebServer.takeRequest().path)
        assertEquals("/v1/performances/", mockWebServer.takeRequest().path)
    }

    @Test
    fun `stops on a network error mid-drain, leaving later ops queued and skipping the reconcile`() = runTest {
        enqueueOp(
            SyncEntityType.PERFORMER, SyncOperationType.CREATE, "perf-1",
            json.encodeToString(PerformerRequest("A", PerformerType.OTHER, id = "perf-1"))
        )
        enqueueOp(
            SyncEntityType.PERFORMER, SyncOperationType.CREATE, "perf-2",
            json.encodeToString(PerformerRequest("B", PerformerType.OTHER, id = "perf-2"))
        )
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = syncManager.sync()

        assertFalse(result.didFinish)
        assertEquals(0, result.numberOfOperationsProcessed)
        // both ops remain queued in FIFO order; the failing op counts an attempt but is NOT flagged
        // failed (a transient error is a pending retry, not a user-facing failure)
        val ops = db.syncOperationDao().getAllOrdered()
        assertEquals(listOf("perf-1", "perf-2"), ops.map { it.entityId })
        assertEquals(1, ops.first().attemptCount)
        assertNull(ops.first().lastError)
        assertEquals(1, mockWebServer.requestCount) // stopped after the first, no reconcile GET
    }

    @Test
    fun `gives up on an op after repeated transient failures so the queue drains past it`() = runTest {
        // perf-1 has already exhausted its retries; this attempt is its last.
        db.syncOperationDao().enqueue(
            SyncOperationEntity(
                entityType = SyncEntityType.PERFORMER.name,
                operationType = SyncOperationType.CREATE.name,
                entityId = "perf-1",
                payloadJson = json.encodeToString(PerformerRequest("A", PerformerType.OTHER, id = "perf-1")),
                createdAt = "2026-01-01T00:00:00Z",
                attemptCount = SyncManager.MAX_ATTEMPTS - 1
            )
        )
        enqueueOp(
            SyncEntityType.PERFORMER, SyncOperationType.CREATE, "perf-2",
            json.encodeToString(PerformerRequest("B", PerformerType.OTHER, id = "perf-2"))
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(500)) // perf-1 keeps failing transiently
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"perf-2","name":"B","type":"OTHER"}"""))
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish) // didn't wedge on perf-1
        assertEquals(1, result.numberOfOperationsProcessed) // perf-2 went through
        val ops = db.syncOperationDao().getAllOrdered()
        assertEquals(listOf("perf-1"), ops.map { it.entityId }) // perf-1 stays, now flagged failed
        assertEquals("SERVER", ops.single().lastError)
    }

    @Test
    fun `drains a DELETE, hard-deleting the tombstoned row`() = runTest {
        db.venueDao().upsert(listOf(VenueEntity("v1", "Hall", "1", "way")))
        db.performanceDao().upsert(
            PerformanceEntity("p-1", "2026-01-01T00:00:00Z", "UPCOMING", "v1", syncState = "PENDING_DELETE")
        )
        enqueueOp(SyncEntityType.PERFORMANCE, SyncOperationType.DELETE, "p-1", null)
        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        mockWebServer.enqueue(reconcileResponse())

        val result = syncManager.sync()

        assertTrue(result.didFinish)
        assertNull(db.performanceDao().getById("p-1"))
        assertTrue(db.syncOperationDao().getAllOrdered().isEmpty())
        assertEquals("DELETE", mockWebServer.takeRequest().method)
    }
}
