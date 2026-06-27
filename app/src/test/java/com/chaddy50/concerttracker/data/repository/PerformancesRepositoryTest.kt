package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.PerformanceRequest
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PerformancesRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()
    private lateinit var repository: PerformancesRepository

    private val performanceJson = """
        {"id":"p1","date":"2024-01-01T19:00:00Z","venue":{"id":"v1","name":"Hall","osm_id":"123","osm_type":"way"},"status":"UPCOMING"}
    """.trimIndent()

    private val performanceListJson = """
        [$performanceJson]
    """.trimIndent()

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        repository = PerformancesRepository(settingsRepository, OkHttpClient(), json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getNextUpcomingPerformance returns Success with performance on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceListJson))
        val result = repository.getNextUpcomingPerformance()
        assertTrue(result is ApiResult.Success)
        assertEquals("p1", (result as ApiResult.Success).data?.id)
    }

    @Test
    fun `getNextUpcomingPerformance returns Success with null on empty list`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val result = repository.getNextUpcomingPerformance()
        assertTrue(result is ApiResult.Success)
        assertNull((result as ApiResult.Success).data)
    }

    @Test
    fun `getNextUpcomingPerformance returns Error on server failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val result = repository.getNextUpcomingPerformance()
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), result)
    }

    @Test
    fun `getRecentlyAttendedPerformances returns Success with list on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceListJson))
        val result = repository.getRecentlyAttendedPerformances()
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `getRecentlyAttendedPerformances returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.getRecentlyAttendedPerformances() is ApiResult.Error)
    }

    @Test
    fun `getUpcomingPerformances returns Success with list on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceListJson))
        val result = repository.getUpcomingPerformances()
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `getUpcomingPerformances returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), repository.getUpcomingPerformances())
    }

    @Test
    fun `getPastPerformances returns Success with list on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceListJson))
        val result = repository.getPastPerformances()
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `getPastPerformances returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.getPastPerformances() is ApiResult.Error)
    }

    @Test
    fun `getPerformance returns Success on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceJson))
        val result = repository.getPerformance("p1")
        assertTrue(result is ApiResult.Success)
        assertEquals("p1", (result as ApiResult.Success).data.id)
    }

    @Test
    fun `getPerformance returns Error on 404`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), repository.getPerformance("p1"))
    }

    @Test
    fun `getPerformance returns Error on network failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.getPerformance("p1") is ApiResult.Error)
    }

    @Test
    fun `createPerformance returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(performanceJson))
        val result = repository.createPerformance(
            PerformanceRequest("2024-01-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        )
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `createPerformance returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.createPerformance(
            PerformanceRequest("2024-01-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        ) is ApiResult.Error)
    }

    @Test
    fun `updatePerformance returns Success on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(performanceJson))
        assertTrue(repository.updatePerformance(
            "p1",
            PerformanceRequest("2024-01-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        ) is ApiResult.Success)
    }

    @Test
    fun `updatePerformance returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.updatePerformance(
            "p1",
            PerformanceRequest("2024-01-01T19:00:00Z", "v1", emptyList(), PerformanceStatus.UPCOMING)
        ) is ApiResult.Error)
    }

    @Test
    fun `deletePerformance returns Success on 204`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        assertTrue(repository.deletePerformance("p1") is ApiResult.Success)
    }

    @Test
    fun `deletePerformance returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repository.deletePerformance("p1") is ApiResult.Error)
    }
}
