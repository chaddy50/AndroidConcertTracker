package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.ComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkComposerEntity
import com.chaddy50.concerttracker.data.local.entity.WorkEntity
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import com.chaddy50.concerttracker.data.local.syncOperationsRepository
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
class WorksRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase

    private lateinit var worksRepository: WorksRepository

    private val workJson = """{"id":"w1","title":"Test Work","composers":[]}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        val client = OkHttpClient()
        db = inMemoryDatabase()
        worksRepository = WorksRepository(
            settingsRepository, client, json, db.workDao(), db.composerDao(),
            db, db.syncOperationsRepository(),
            javax.inject.Provider { mockk<com.chaddy50.concerttracker.data.sync.SyncScheduler>(relaxed = true) }
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    @Test
    fun `findOrCreateWork returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(workJson))
        assertTrue(worksRepository.findOrCreateWork("1", "Symphony No. 5", null, "2", "Beethoven") is ApiResult.Success)
    }

    @Test
    fun `findOrCreateWork returns Success with existing work on 409 with body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(workJson))
        val result = worksRepository.findOrCreateWork("1", "Symphony No. 5", null, "2", "Beethoven")
        assertTrue(result is ApiResult.Success)
        assertEquals("w1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `findOrCreateWork returns Error on 409 without parseable body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(""))
        assertTrue(worksRepository.findOrCreateWork("1", "Symphony No. 5", null, "2", "Beethoven") is ApiResult.Error)
    }

    @Test
    fun `findOrCreateWork returns Error on non-409 HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        assertTrue(worksRepository.findOrCreateWork("1", "Symphony No. 5", null, "2", "Beethoven") is ApiResult.Error)
    }

    @Test
    fun `findOrCreateWork for a custom work under a cached composer is local-first, nesting the composer by our id`() = runTest {
        db.composerDao().upsert(listOf(ComposerEntity("c-existing", "Custom Composer")))

        val result = worksRepository.findOrCreateWork(null, "Custom Work", "c-existing", null, "Custom Composer")

        assertTrue(result is ApiResult.Success)
        val work = (result as ApiResult.Success).data
        // work persisted and linked to the existing composer; nothing sent over the network
        assertEquals(listOf("c-existing"), worksRepository.searchWorksForComposer("c-existing", "").first().single().composers.map { it.id })
        assertEquals(0, mockWebServer.requestCount)
        val op = db.syncOperationDao().getAllOrdered().single()
        assertEquals("WORK", op.entityType)
        assertEquals("CREATE", op.operationType)
        assertTrue("composer nested by our id", op.payloadJson!!.contains("\"id\":\"c-existing\""))
        assertTrue("work id in payload", op.payloadJson!!.contains(work.id))
    }

    @Test
    fun `findOrCreateWork for a custom work with a fresh custom composer persists both and enqueues one WORK op`() = runTest {
        val result = worksRepository.findOrCreateWork(null, "My Work", null, null, "New Composer")

        assertTrue(result is ApiResult.Success)
        val work = (result as ApiResult.Success).data
        val composerId = work.composers.single().id
        assertEquals("New Composer", db.composerDao().getById(composerId)?.name)
        assertEquals(listOf(work.id), worksRepository.searchWorksForComposer(composerId, "").first().map { it.id })
        assertEquals(0, mockWebServer.requestCount)
        assertEquals(listOf("WORK"), db.syncOperationDao().getAllOrdered().map { it.entityType })
    }

    @Test
    fun `searchWorksForComposer emits the composer's works with composers populated`() = runTest {
        db.composerDao().upsert(listOf(ComposerEntity("c1", "Beethoven")))
        db.workDao().upsert(listOf(WorkEntity("w1", "Symphony No. 5")))
        db.workDao().upsertWorkComposers(listOf(WorkComposerEntity("w1", "c1")))

        val works = worksRepository.searchWorksForComposer("c1", "").first()

        assertEquals(listOf("w1"), works.map { it.id })
        assertEquals(listOf("c1"), works.single().composers.map { it.id })
    }

    @Test
    fun `searchWorksForComposer scopes to the composer and filters by title`() = runTest {
        db.composerDao().upsert(listOf(ComposerEntity("c1", "Beethoven"), ComposerEntity("c2", "Mozart")))
        db.workDao().upsert(
            listOf(
                WorkEntity("w1", "Symphony No. 5"),
                WorkEntity("w2", "Missa Solemnis"),
                WorkEntity("w3", "Requiem")
            )
        )
        db.workDao().upsertWorkComposers(
            listOf(WorkComposerEntity("w1", "c1"), WorkComposerEntity("w2", "c1"), WorkComposerEntity("w3", "c2"))
        )

        assertEquals(
            listOf("w1"),
            worksRepository.searchWorksForComposer("c1", "symph").first().map { it.id }
        )
    }
}
