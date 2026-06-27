package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.MusicBrainzApiService
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.data.enum.PerformerType
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

class MusicBrainzRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val json = testJson()
    private lateinit var repository: MusicBrainzRepository

    @Before
    fun setUp() {
        mockWebServer.start()
        val apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MusicBrainzApiService::class.java)
        repository = MusicBrainzRepository(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `search maps artist types to performer types for PERFORMER`() = runTest {
        val body = """{"artists":[""" +
            """{"id":"a1","name":"Berlin Phil","type":"Orchestra"},""" +
            """{"id":"a2","name":"Vienna Choir","type":"Choir"},""" +
            """{"id":"a3","name":"Some Quartet","type":"Group"},""" +
            """{"id":"a4","name":"Glenn Gould","type":"Person","disambiguation":"pianist"},""" +
            """{"id":"a5","name":"Mystery","type":"Other"},""" +
            """{"id":"a6","name":"No Type"}]}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = repository.search(MusicBrainzEntityType.PERFORMER, "x")

        assertTrue(result is ApiResult.Success)
        val results = (result as ApiResult.Success).data
        assertEquals(
            listOf(
                PerformerType.ORCHESTRA,
                PerformerType.CHORUS,
                PerformerType.ENSEMBLE,
                PerformerType.SOLO,
                PerformerType.OTHER,
                PerformerType.OTHER
            ),
            results.map { it.performerType }
        )
        assertEquals("pianist", results.first { it.id == "a4" }.description)
    }

    @Test
    fun `search forces CONDUCTOR type regardless of artist type`() = runTest {
        val body = """{"artists":[{"id":"a1","name":"Karajan","type":"Person"}]}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = repository.search(MusicBrainzEntityType.CONDUCTOR, "karajan")

        assertTrue(result is ApiResult.Success)
        assertEquals(
            listOf(PerformerType.CONDUCTOR),
            (result as ApiResult.Success).data.map { it.performerType }
        )
    }

    @Test
    fun `search returns Error SERVER on 500`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertEquals(
            ApiResult.Error(ApiErrorType.Type.SERVER),
            repository.search(MusicBrainzEntityType.PERFORMER, "x")
        )
    }
}
