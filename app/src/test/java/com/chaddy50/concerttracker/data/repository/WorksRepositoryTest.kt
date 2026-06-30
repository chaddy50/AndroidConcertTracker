package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import io.mockk.every
import io.mockk.mockk
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

class WorksRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()

    private lateinit var worksRepository: WorksRepository

    private val workJson = """{"id":"w1","title":"Test Work","composers":[]}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        val client = OkHttpClient()
        worksRepository = WorksRepository(settingsRepository, client, json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchWorks returns Success on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[$workJson]"))
        assertTrue(worksRepository.searchWorks("symphony") is ApiResult.Success)
    }

    @Test
    fun `searchWorks returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(worksRepository.searchWorks("symphony") is ApiResult.Error)
    }

    @Test
    fun `createWorkFromOpenOpus returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(workJson))
        assertTrue(worksRepository.createWorkFromOpenOpus("1", "Symphony No. 5", "2", "Beethoven") is ApiResult.Success)
    }

    @Test
    fun `createWorkFromOpenOpus returns Success with existing work on 409 with body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(workJson))
        val result = worksRepository.createWorkFromOpenOpus("1", "Symphony No. 5", "2", "Beethoven")
        assertTrue(result is ApiResult.Success)
        assertEquals("w1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `createWorkFromOpenOpus returns Error on 409 without parseable body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(""))
        assertTrue(worksRepository.createWorkFromOpenOpus("1", "Symphony No. 5", "2", "Beethoven") is ApiResult.Error)
    }

    @Test
    fun `createWorkFromOpenOpus returns Error on non-409 HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        assertTrue(worksRepository.createWorkFromOpenOpus("1", "Symphony No. 5", "2", "Beethoven") is ApiResult.Error)
    }
}
