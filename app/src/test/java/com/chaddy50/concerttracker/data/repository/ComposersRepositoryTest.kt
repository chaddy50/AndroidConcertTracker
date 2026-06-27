package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import com.chaddy50.concerttracker.testJson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ComposersRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val settingsRepository: SettingsRepository = mockk()
    private val json = testJson()

    private lateinit var composersRepository: ComposersRepository

    private val composerJson = """{"id":"c1","name":"Test Composer"}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        every { settingsRepository.serverUrl } returns flowOf(mockWebServer.url("/").toString().trimEnd('/'))
        val client = OkHttpClient()
        composersRepository = ComposersRepository(settingsRepository, client, json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchComposers returns Success on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[$composerJson]"))
        assertTrue(composersRepository.searchComposers("bach") is ApiResult.Success)
    }

    @Test
    fun `searchComposers returns Error on failure`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertTrue(composersRepository.searchComposers("bach") is ApiResult.Error)
    }
}
