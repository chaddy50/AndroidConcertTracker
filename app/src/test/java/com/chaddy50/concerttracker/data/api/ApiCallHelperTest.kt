package com.chaddy50.concerttracker.data.api

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiCallHelperTest {

    @Test
    fun `returns Success with lambda return value when block succeeds`() = runTest {
        val result = safeApiCall { "data" }
        assertTrue(result is ApiResult.Success)
        assertEquals("data", (result as ApiResult.Success).data)
    }

    @Test
    fun `returns Error NETWORK when block throws UnknownHostException`() = runTest {
        val result = safeApiCall<String> { throw UnknownHostException() }
        assertEquals(ApiResult.Error(ApiErrorType.Type.NETWORK), result)
    }

    @Test
    fun `returns Error NETWORK when block throws ConnectException`() = runTest {
        val result = safeApiCall<String> { throw ConnectException() }
        assertEquals(ApiResult.Error(ApiErrorType.Type.NETWORK), result)
    }

    @Test
    fun `returns Error NETWORK when block throws IOException`() = runTest {
        val result = safeApiCall<String> { throw IOException() }
        assertEquals(ApiResult.Error(ApiErrorType.Type.NETWORK), result)
    }

    @Test
    fun `returns Error TIMEOUT when block throws SocketTimeoutException`() = runTest {
        val result = safeApiCall<String> { throw SocketTimeoutException() }
        assertEquals(ApiResult.Error(ApiErrorType.Type.TIMEOUT), result)
    }

    @Test
    fun `returns Error SERVER when block throws HttpException with 500 code`() = runTest {
        val result = safeApiCall<String> { throw httpException(500) }
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), result)
    }

    @Test
    fun `returns Error SERVER when block throws HttpException with 503 code`() = runTest {
        val result = safeApiCall<String> { throw httpException(503) }
        assertEquals(ApiResult.Error(ApiErrorType.Type.SERVER), result)
    }

    @Test
    fun `returns Error CLIENT when block throws HttpException with 400 code`() = runTest {
        val result = safeApiCall<String> { throw httpException(400) }
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), result)
    }

    @Test
    fun `returns Error CLIENT when block throws HttpException with 404 code`() = runTest {
        val result = safeApiCall<String> { throw httpException(404) }
        assertEquals(ApiResult.Error(ApiErrorType.Type.CLIENT), result)
    }

    @Test
    fun `returns Error CONFLICT when block throws HttpException with 409 code`() = runTest {
        val result = safeApiCall<String> { throw httpException(409) }
        assertEquals(ApiResult.Error(ApiErrorType.Type.CONFLICT), result)
    }

    @Test
    fun `returns Error UNKNOWN when block throws unrecognized exception`() = runTest {
        val result = safeApiCall<String> { throw IllegalStateException("unexpected") }
        assertEquals(ApiResult.Error(ApiErrorType.Type.UNKNOWN), result)
    }

    @Test
    fun `works correctly as a suspending function`() = runTest {
        var executed = false
        safeApiCall { executed = true }
        assertTrue(executed)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody()))
}
