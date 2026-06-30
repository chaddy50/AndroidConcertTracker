package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import com.chaddy50.concerttracker.testJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class OpenOpusRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val json = testJson()
    private lateinit var repository: OpenOpusRepository

    @Before
    fun setUp() {
        mockWebServer.start()
        val apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenOpusApiService::class.java)
        repository = OpenOpusRepository(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchComposers returns Success with composers on 200`() = runTest {
        val body = """{"status":{"success":"true","rows":1},"composers":[""" +
            """{"id":"1","name":"Bach","complete_name":"Johann Sebastian Bach","epoch":"Baroque"}]}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val result = repository.searchComposers("bach")
        assertTrue(result is ApiResult.Success)
        assertEquals(listOf("Bach"), (result as ApiResult.Success).data.map { it.name })
    }

    @Test
    fun `searchComposers returns Error SERVER on 500`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), repository.searchComposers("bach"))
    }

    @Test
    fun `getWorksByComposer returns works sorted by title on 200`() = runTest {
        val body = """{"status":{"success":"true","rows":2},"works":[""" +
            """{"id":"2","title":"Zelda Suite"},{"id":"1","title":"Aria"}]}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val result = repository.getWorksByComposer("1")
        assertTrue(result is ApiResult.Success)
        assertEquals(listOf("Aria", "Zelda Suite"), (result as ApiResult.Success).data.map { it.title })
    }

    @Test
    fun `getWorksByComposer returns Error CLIENT on 400`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), repository.getWorksByComposer("1"))
    }
}
