package com.chaddy50.concerttracker.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResultTest {

    @Test
    fun `ApiResult_Success holds provided data and is Success subtype`() {
        val result = ApiResult.Success("hello")
        assertTrue(result is ApiResult.Success)
        assertEquals("hello", result.data)
    }

    @Test
    fun `ApiResult_Error holds provided ErrorType and is Error subtype`() {
        val result = ApiResult.Error(ApiErrorType.Type.NETWORK)
        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorType.Type.NETWORK, result.errorType)
    }

    @Test
    fun `ApiResult is generic and works with list payload`() {
        val result: ApiResult<List<String>> = ApiResult.Success(listOf("a", "b"))
        assertTrue(result is ApiResult.Success)
        assertEquals(listOf("a", "b"), (result as ApiResult.Success).data)
    }

    @Test
    fun `ApiResult is generic and works with Unit payload`() {
        val result: ApiResult<Unit> = ApiResult.Success(Unit)
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `onSuccess invokes action when result is Success`() {
        var called = false
        ApiResult.Success(42).onSuccess { called = true }
        assertTrue(called)
    }

    @Test
    fun `onSuccess passes data value to action`() {
        var received: Int? = null
        ApiResult.Success(42).onSuccess { received = it }
        assertEquals(42, received)
    }

    @Test
    fun `onSuccess does not invoke action when result is Error`() {
        var called = false
        ApiResult.Error(ApiErrorType.Type.NETWORK).onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onSuccess returns the same instance`() {
        val result = ApiResult.Success(1)
        assertSame(result, result.onSuccess { })
    }

    @Test
    fun `onError invokes action when result is Error`() {
        var called = false
        ApiResult.Error(ApiErrorType.Type.TIMEOUT).onError { called = true }
        assertTrue(called)
    }

    @Test
    fun `onError passes ErrorType to action`() {
        var received: ApiErrorType.Type? = null
        ApiResult.Error(ApiErrorType.Type.SERVER).onError { received = it }
        assertEquals(ApiErrorType.Type.SERVER, received)
    }

    @Test
    fun `onError does not invoke action when result is Success`() {
        var called = false
        ApiResult.Success("x").onError { called = true }
        assertFalse(called)
    }

    @Test
    fun `onError returns the same instance`() {
        val result = ApiResult.Error(ApiErrorType.Type.UNKNOWN)
        assertSame(result, result.onError { })
    }
}
