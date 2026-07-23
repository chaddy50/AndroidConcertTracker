package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.PerformerRequest
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformersRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val syncScheduler: com.chaddy50.concerttracker.data.sync.SyncScheduler = mockk(relaxed = true)
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var repository: PerformersRepository

    private val performerJson = """{"id":"pe1","name":"Test Performer","type":"ORCHESTRA"}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        db = inMemoryDatabase()
        repository = PerformersRepository(
            settingsRepository, OkHttpClient(), json, db.performerDao(),
            db, db.syncOperationsRepository(), javax.inject.Provider { syncScheduler }
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    // A catalog performer (has a musicbrainzId) still find-or-creates online.
    private fun catalogRequest() = PerformerRequest("Test", PerformerType.ORCHESTRA, musicbrainzId = "mb1")

    @Test
    fun `findOrCreatePerformer for a catalog performer returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(performerJson))
        val result = repository.findOrCreatePerformer(catalogRequest())
        assertTrue(result is ApiResult.Success)
        assertEquals("pe1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `findOrCreatePerformer for a catalog performer treats 409-with-body as Success`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(performerJson))
        val result = repository.findOrCreatePerformer(catalogRequest())
        assertTrue(result is ApiResult.Success)
        assertEquals("pe1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `findOrCreatePerformer for a catalog performer returns Error on non-409`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), repository.findOrCreatePerformer(catalogRequest()))
    }

    @Test
    fun `findOrCreatePerformer for a catalog performer writes it through to Room on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(performerJson))

        repository.findOrCreatePerformer(catalogRequest())

        assertEquals("Test Performer", db.performerDao().getById("pe1")?.name)
        assertEquals(listOf("pe1"), repository.searchPerformers("").first().map { it.id })
    }

    @Test
    fun `findOrCreatePerformer for a catalog performer writes it through to Room on 409-with-body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(performerJson))

        repository.findOrCreatePerformer(catalogRequest())

        assertEquals("Test Performer", db.performerDao().getById("pe1")?.name)
    }

    @Test
    fun `findOrCreatePerformer for a catalog performer writes nothing to Room on error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        repository.findOrCreatePerformer(catalogRequest())

        assertTrue(repository.searchPerformers("").first().isEmpty())
    }

    @Test
    fun `findOrCreatePerformer for a custom performer is local-first, enqueues one op, no network`() = runTest {
        val result = repository.findOrCreatePerformer(PerformerRequest("My Soloist", PerformerType.OTHER))

        assertTrue(result is ApiResult.Success)
        val created = (result as ApiResult.Success).data
        assertEquals("My Soloist", db.performerDao().getById(created.id)?.name)
        assertEquals(0, mockWebServer.requestCount)
        val op = db.syncOperationDao().getAllOrdered().single()
        assertEquals("PERFORMER", op.entityType)
        assertEquals("CREATE", op.operationType)
        assertEquals(created.id, op.entityId)
        assertTrue(op.payloadJson!!.contains(created.id))
    }

    @Test
    fun `findOrCreatePerformer for a custom conductor persists and is findable with type preserved`() = runTest {
        val result = repository.findOrCreatePerformer(PerformerRequest("My Conductor", PerformerType.CONDUCTOR))

        assertTrue(result is ApiResult.Success)
        val created = (result as ApiResult.Success).data
        val found = repository.searchPerformers("").first().single()
        assertEquals(created.id, found.id)
        assertEquals(PerformerType.CONDUCTOR, found.type)
    }

    @Test
    fun `searchPerformers emits cached performers mapped to domain`() = runTest {
        db.performerDao().upsert(listOf(PerformerEntity("pe1", "Berlin Philharmonic", "ORCHESTRA")))
        val performers = repository.searchPerformers("").first()
        assertEquals(listOf("pe1"), performers.map { it.id })
        assertEquals(PerformerType.ORCHESTRA, performers.single().type)
    }

    @Test
    fun `searchPerformers filters by case-insensitive name substring`() = runTest {
        db.performerDao().upsert(
            listOf(
                PerformerEntity("pe1", "Berlin Philharmonic", "ORCHESTRA"),
                PerformerEntity("pe2", "Yo-Yo Ma", "SOLO")
            )
        )
        assertEquals(listOf("pe1"), repository.searchPerformers("phil").first().map { it.id })
    }

    @Test
    fun `searchPerformers includes performers with null musicbrainzId`() = runTest {
        db.performerDao().upsert(listOf(PerformerEntity("pe1", "Custom", "OTHER", musicbrainzId = null)))
        assertEquals(listOf("pe1"), repository.searchPerformers("").first().map { it.id })
    }

    @Test
    fun `updatePerformer returns Success with the updated performer`() = runTest {
        db.performerDao().upsert(listOf(PerformerEntity("pe1", "Old Name", "SOLO", "Pianist")))

        val result = repository.updatePerformer("pe1", "Old Name", PerformerType.CONDUCTOR, "Conductor", null)

        assertTrue(result is ApiResult.Success)
        val performer = (result as ApiResult.Success).data
        assertEquals("pe1", performer.id)
        assertEquals(PerformerType.CONDUCTOR, performer.type)
        assertEquals("Conductor", performer.specialty)
    }

    @Test
    fun `updatePerformer writes the correction through to Room`() = runTest {
        db.performerDao().upsert(listOf(PerformerEntity("pe1", "Yo-Yo Ma", "SOLO", "Pianist")))

        repository.updatePerformer("pe1", "Yo-Yo Ma", PerformerType.SOLO, "Cellist", null)

        val row = db.performerDao().getById("pe1")
        assertEquals("SOLO", row?.type)
        assertEquals("Cellist", row?.specialty)
    }

    @Test
    fun `updatePerformer upserts in place rather than inserting a duplicate`() = runTest {
        db.performerDao().upsert(listOf(PerformerEntity("pe1", "Name", "SOLO", "Pianist")))

        repository.updatePerformer("pe1", "Name", PerformerType.CONDUCTOR, "Conductor", null)

        val all = repository.searchPerformers("").first()
        assertEquals(1, all.size)
        assertEquals(PerformerType.CONDUCTOR, all.single().type)
        assertEquals("Conductor", all.single().specialty)
    }

    @Test
    fun `updatePerformer correction is visible through searchPerformers everywhere`() = runTest {
        db.performerDao().upsert(listOf(PerformerEntity("pe1", "Andris Nelsons", "SOLO", null)))

        repository.updatePerformer("pe1", "Andris Nelsons", PerformerType.CONDUCTOR, "Conductor", null)

        val found = repository.searchPerformers("").first().single()
        assertEquals(PerformerType.CONDUCTOR, found.type)
        assertEquals("Conductor", found.specialty)
    }

    @Test
    fun `updatePerformer enqueues one PERFORMER UPDATE op with the id`() = runTest {
        repository.updatePerformer("pe1", "Name", PerformerType.SOLO, "Cellist", null)

        val op = db.syncOperationDao().getAllOrdered().single()
        assertEquals("PERFORMER", op.entityType)
        assertEquals("UPDATE", op.operationType)
        assertEquals("pe1", op.entityId)
        val request = json.decodeFromString<PerformerRequest>(op.payloadJson!!)
        assertEquals("pe1", request.id)
        assertEquals("Name", request.name)
        assertEquals(PerformerType.SOLO, request.type)
        assertEquals("Cellist", request.specialty)
    }

    @Test
    fun `updatePerformer makes no direct network request`() = runTest {
        repository.updatePerformer("pe1", "Name", PerformerType.SOLO, "Cellist", null)
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `updatePerformer requests a sync`() = runTest {
        repository.updatePerformer("pe1", "Name", PerformerType.SOLO, "Cellist", null)
        io.mockk.coVerify(exactly = 1) { syncScheduler.requestSync() }
    }

    @Test
    fun `updatePerformer with null specialty stores null locally but encodes an empty string to clear it`() = runTest {
        repository.updatePerformer("pe1", "Name", PerformerType.CONDUCTOR, null, null)

        // Room keeps the clean null; the wire carries "" so the clear isn't dropped as an omitted field.
        assertNull(db.performerDao().getById("pe1")?.specialty)
        val op = db.syncOperationDao().getAllOrdered().single()
        assertEquals("", json.decodeFromString<PerformerRequest>(op.payloadJson!!).specialty)
    }

    @Test
    fun `updatePerformer preserves musicbrainzId in the row and payload`() = runTest {
        repository.updatePerformer("pe1", "Name", PerformerType.SOLO, "Cellist", "mb1")

        assertEquals("mb1", db.performerDao().getById("pe1")?.musicbrainzId)
        val op = db.syncOperationDao().getAllOrdered().single()
        assertEquals("mb1", json.decodeFromString<PerformerRequest>(op.payloadJson!!).musicbrainzId)
    }

    @Test
    fun `updatePerformer commits the row upsert and the op enqueue together`() = runTest {
        repository.updatePerformer("pe1", "Name", PerformerType.SOLO, "Cellist", null)

        assertNotNull(db.performerDao().getById("pe1"))
        assertEquals(1, db.syncOperationDao().getAllOrdered().size)
    }
}
