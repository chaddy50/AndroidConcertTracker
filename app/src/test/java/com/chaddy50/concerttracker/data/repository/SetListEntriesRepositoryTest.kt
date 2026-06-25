package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.api.SetListEntryRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetListEntriesRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var setListEntriesRepository: SetListEntriesRepository

    private val workJson = """{"id":"w1","title":"Test Work","composers":[]}"""
    private val setListEntryJson = """{"id":"s1","work":$workJson,"order":1}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        val client = OkHttpClient()
        setListEntriesRepository = SetListEntriesRepository(settingsRepository, client, json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `createSetListEntry returns Success on 201`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(setListEntryJson))
        assertTrue(setListEntriesRepository.createSetListEntry(
            SetListEntryCreateRequest("p1", "w1", 1, emptyList())
        ) is ApiResult.Success)
    }

    @Test
    fun `createSetListEntry returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(setListEntriesRepository.createSetListEntry(
            SetListEntryCreateRequest("p1", "w1", 1, emptyList())
        ) is ApiResult.Error)
    }

    @Test
    fun `updateSetListEntry returns Success on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(setListEntryJson))
        assertTrue(setListEntriesRepository.updateSetListEntry("s1", SetListEntryRequest(notes = "great")) is ApiResult.Success)
    }

    @Test
    fun `updateSetListEntry returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(setListEntriesRepository.updateSetListEntry("s1", SetListEntryRequest()) is ApiResult.Error)
    }

    @Test
    fun `deleteSetListEntry returns Success on 204`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        assertTrue(setListEntriesRepository.deleteSetListEntry("s1") is ApiResult.Success)
    }

    @Test
    fun `deleteSetListEntry returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(setListEntriesRepository.deleteSetListEntry("s1") is ApiResult.Error)
    }
}
