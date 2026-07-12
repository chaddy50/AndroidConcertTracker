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
}
