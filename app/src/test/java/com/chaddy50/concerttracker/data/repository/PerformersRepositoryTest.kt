package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.PerformerRequest
import com.chaddy50.concerttracker.data.enum.PerformerType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PerformersRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var repository: PerformersRepository

    private val performerJson = """{"id":"pe1","name":"Test Performer","type":"ORCHESTRA"}"""
    private val performerListJson = """[$performerJson]"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        repository = PerformersRepository(settingsRepository, OkHttpClient(), json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchPerformers returns Success with list on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performerListJson))
        val result = repository.searchPerformers("test")
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `searchPerformers sends null name for blank query`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        repository.searchPerformers("")
        val request = mockWebServer.takeRequest()
        assertTrue(!request.path!!.contains("name="))
    }

    @Test
    fun `searchPerformers returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.searchPerformers("test") is ApiResult.Error)
    }

    @Test
    fun `createPerformer returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(performerJson))
        val result = repository.createPerformer(PerformerRequest("Test", PerformerType.ORCHESTRA))
        assertTrue(result is ApiResult.Success)
        assertEquals("pe1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `createPerformer returns Success with existing performer on 409 with body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(performerJson))
        val result = repository.createPerformer(PerformerRequest("Test", PerformerType.ORCHESTRA))
        assertTrue(result is ApiResult.Success)
        assertEquals("pe1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `createPerformer returns Error on 409 without parseable body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(""))
        val result = repository.createPerformer(PerformerRequest("Test", PerformerType.ORCHESTRA))
        assertTrue(result is ApiResult.Error)
    }

    @Test
    fun `createPerformer returns Error on non-409 HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), repository.createPerformer(PerformerRequest("Test", PerformerType.ORCHESTRA)))
    }

    @Test
    fun `createPerformer returns Error on network failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), repository.createPerformer(PerformerRequest("Test", PerformerType.ORCHESTRA)))
    }
}
