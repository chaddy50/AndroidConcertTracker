package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import com.chaddy50.concerttracker.data.local.syncOperationsRepository
import com.chaddy50.concerttracker.data.sync.SyncScheduler
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
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetListEntriesRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val syncScheduler: SyncScheduler = mockk(relaxed = true)
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var performancesRepository: PerformancesRepository
    private lateinit var repository: SetListEntriesRepository

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
            db.performerDao(), db.workDao(), db.composerDao(),
            db.syncOperationsRepository(), Provider { syncScheduler }
        )
        repository = SetListEntriesRepository(
            json, db, db.setListEntryDao(), db.syncOperationsRepository(), performancesRepository, Provider { syncScheduler }
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    /** Seed a synced parent performance (with entry p1_s1 + cached work w1) via the online pull. */
    private suspend fun seedParent() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[${parentJson(notes = null)}]"))
        performancesRepository.loadPerformances()
    }

    @Test
    fun `createSetListEntry persists a PENDING entry, re-emits the parent, and enqueues one CREATE op`() = runTest {
        seedParent()
        val requestsBefore = mockWebServer.requestCount

        val result = repository.createSetListEntry(SetListEntryCreateRequest("p1", "w1", 2, emptyList()))

        assertTrue(result is ApiResult.Success)
        val created = (result as ApiResult.Success).data
        val cached = performancesRepository.observePerformance("p1").first()
        assertTrue(cached!!.setList.any { it.id == created.id })
        assertEquals("PENDING", db.setListEntryDao().getById(created.id)?.syncState)
        assertEquals(requestsBefore, mockWebServer.requestCount)
        val op = db.syncOperationDao().getAllOrdered().single { it.entityId == created.id }
        assertEquals("SET_LIST_ENTRY", op.entityType)
        assertEquals("CREATE", op.operationType)
    }

    @Test
    fun `updateSetListEntry sets notes in place as PENDING with one UPDATE op, no network`() = runTest {
        seedParent()
        val requestsBefore = mockWebServer.requestCount

        val result = repository.updateSetListEntry("p1_s1", "Great")

        assertTrue(result is ApiResult.Success)
        assertEquals("Great", performancesRepository.observePerformance("p1").first()?.setList?.single()?.notes)
        assertEquals("PENDING", db.setListEntryDao().getById("p1_s1")?.syncState)
        assertEquals(requestsBefore, mockWebServer.requestCount)
        assertEquals(
            listOf("UPDATE"),
            db.syncOperationDao().getAllOrdered().filter { it.entityId == "p1_s1" }.map { it.operationType }
        )
    }

    @Test
    fun `deleteSetListEntry of a synced entry tombstones it and enqueues a DELETE op`() = runTest {
        seedParent()
        val requestsBefore = mockWebServer.requestCount

        val result = repository.deleteSetListEntry("p1_s1")

        assertTrue(result is ApiResult.Success)
        assertTrue(performancesRepository.observePerformance("p1").first()?.setList?.isEmpty() == true)
        assertEquals("PENDING_DELETE", db.setListEntryDao().getById("p1_s1")?.syncState)
        assertEquals(requestsBefore, mockWebServer.requestCount)
        assertEquals("DELETE", db.syncOperationDao().getAllOrdered().single { it.entityId == "p1_s1" }.operationType)
    }

    @Test
    fun `create then delete of an unsynced entry collapses to no queued op`() = runTest {
        seedParent()
        val created = (repository.createSetListEntry(
            SetListEntryCreateRequest("p1", "w1", 2, emptyList())
        ) as ApiResult.Success).data

        repository.deleteSetListEntry(created.id)

        assertNull(db.setListEntryDao().getById(created.id))
        assertTrue(db.syncOperationDao().getAllOrdered().none { it.entityId == created.id })
    }
}
