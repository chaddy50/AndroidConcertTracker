package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.testJson
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerValidationRepositoryTest {

    private val mockWebServer = MockWebServer()
    private lateinit var repository: ServerValidationRepository

    private fun baseUrl() = mockWebServer.url("/").toString().trimEnd('/')

    @Before
    fun setUp() {
        mockWebServer.start()
        repository = ServerValidationRepository(OkHttpClient(), testJson())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // region validate — success

    @Test
    fun `validate returns Success on 200 with a JSON array body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = repository.validate(baseUrl())

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `validate probes the performances endpoint with limit 1`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        repository.validate(baseUrl())

        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.startsWith("/v1/performances"))
        assertTrue(request.path!!.contains("limit=1"))
    }

    @Test
    fun `validate handles a trailing slash in the input url`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = repository.validate("${baseUrl()}/")

        assertTrue(result is ApiResult.Success)
        assertTrue(mockWebServer.takeRequest().path!!.startsWith("/v1/performances"))
    }

    // endregion

    // region validate — failure

    @Test
    fun `validate returns Server error on 500`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = repository.validate(baseUrl())

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorType.Type.SERVER, (result as ApiResult.Error).errorType)
    }

    @Test
    fun `validate returns Client error on 404 for a reachable non-server`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val result = repository.validate(baseUrl())

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorType.Type.CLIENT, (result as ApiResult.Error).errorType)
    }

    @Test
    fun `validate returns an Error when the host is unreachable`() = runTest {
        mockWebServer.shutdown() // nothing is listening

        val result = repository.validate(baseUrl())

        assertTrue(result is ApiResult.Error)
    }

    @Test
    fun `validate returns an Error and does not throw on a non-JSON 200 body`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("<html>not json</html>"))

        val result = repository.validate(baseUrl())

        assertTrue(result is ApiResult.Error)
    }

    @Test
    fun `validate returns an Error for a malformed url rather than throwing`() = runTest {
        val result = repository.validate("http://")

        assertTrue(result is ApiResult.Error)
    }

    // endregion

    // region isValidServerUrlFormat

    @Test
    fun `isValidServerUrlFormat accepts http and https urls`() {
        assertTrue("http://192.168.1.100:3000".isValidServerUrlFormat())
        assertTrue("https://server.example".isValidServerUrlFormat())
        assertTrue("https://host.example:8443/api".isValidServerUrlFormat())
        assertTrue("HTTP://host.example".isValidServerUrlFormat())
    }

    @Test
    fun `isValidServerUrlFormat rejects blank, scheme-less, and non-http urls`() {
        assertFalse("".isValidServerUrlFormat())
        assertFalse("   ".isValidServerUrlFormat())
        assertFalse("192.168.1.100:3000".isValidServerUrlFormat())
        assertFalse("ftp://host.example".isValidServerUrlFormat())
        assertFalse("mailto:someone@example.com".isValidServerUrlFormat())
        assertFalse("http://".isValidServerUrlFormat())
    }

    // endregion

    // region error mapping

    @Test
    fun `error mapping covers every ApiErrorType and buckets as intended`() {
        assertEquals(ServerUrlValidationError.UNREACHABLE, ApiErrorType.Type.NETWORK.toServerUrlValidationError())
        assertEquals(ServerUrlValidationError.UNREACHABLE, ApiErrorType.Type.TIMEOUT.toServerUrlValidationError())
        assertEquals(ServerUrlValidationError.SERVER_ERROR, ApiErrorType.Type.SERVER.toServerUrlValidationError())
        assertEquals(ServerUrlValidationError.SERVER_ERROR, ApiErrorType.Type.CLIENT.toServerUrlValidationError())
        assertEquals(ServerUrlValidationError.UNKNOWN, ApiErrorType.Type.CONFLICT.toServerUrlValidationError())
        assertEquals(ServerUrlValidationError.UNKNOWN, ApiErrorType.Type.UNKNOWN.toServerUrlValidationError())
        // Every enum value maps without throwing.
        ApiErrorType.Type.entries.forEach { it.toServerUrlValidationError() }
    }

    // endregion
}
