package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.PerformerRequest
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.PerformerEntity
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
class PerformersRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var db: ConcertTrackerDatabase
    private lateinit var repository: PerformersRepository

    private val performerJson = """{"id":"pe1","name":"Test Performer","type":"ORCHESTRA"}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        db = inMemoryDatabase()
        repository = PerformersRepository(settingsRepository, OkHttpClient(), json, db.performerDao())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        db.close()
    }

    @Test
    fun `findOrCreatePerformer returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(performerJson))
        val result = repository.findOrCreatePerformer(PerformerRequest("Test", PerformerType.ORCHESTRA))
        assertTrue(result is ApiResult.Success)
        assertEquals("pe1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `findOrCreatePerformer returns Success with existing performer on 409 with body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(performerJson))
        val result = repository.findOrCreatePerformer(PerformerRequest("Test", PerformerType.ORCHESTRA))
        assertTrue(result is ApiResult.Success)
        assertEquals("pe1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `findOrCreatePerformer returns Error on 409 without parseable body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(""))
        val result = repository.findOrCreatePerformer(PerformerRequest("Test", PerformerType.ORCHESTRA))
        assertTrue(result is ApiResult.Error)
    }

    @Test
    fun `findOrCreatePerformer returns Error on non-409 HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), repository.findOrCreatePerformer(PerformerRequest("Test", PerformerType.ORCHESTRA)))
    }

    @Test
    fun `findOrCreatePerformer returns Error on network failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), repository.findOrCreatePerformer(PerformerRequest("Test", PerformerType.ORCHESTRA)))
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
