package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.NominatimApiService
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

class NominatimRepositoryTest {

    private val mockWebServer = MockWebServer()
    private val json = testJson()
    private lateinit var repository: NominatimRepository

    private val venueJson = """{"osm_id":123,"osm_type":"way","display_name":"Test Hall, City","name":"Test Hall"}"""

    @Before
    fun setUp() {
        mockWebServer.start()
        val apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NominatimApiService::class.java)
        repository = NominatimRepository(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchVenues returns Success with results on 200`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[$venueJson]"))
        val result = repository.searchVenues("hall")
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
        assertEquals("Test Hall", result.data.first().name)
    }

    @Test
    fun `searchVenues filters out results with blank names`() = runTest {
        val blankNameJson = """{"osm_id":456,"osm_type":"node","display_name":"Unnamed","name":""}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[$venueJson,$blankNameJson]"))
        val result = repository.searchVenues("hall")
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
        assertEquals("Test Hall", result.data.first().name)
    }

    @Test
    fun `searchVenues returns empty Success when no results`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val result = repository.searchVenues("nothing")
        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun `searchVenues returns Error SERVER on 500`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), repository.searchVenues("hall"))
    }

    @Test
    fun `searchVenues returns Error CLIENT on 400`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), repository.searchVenues("hall"))
    }
}
