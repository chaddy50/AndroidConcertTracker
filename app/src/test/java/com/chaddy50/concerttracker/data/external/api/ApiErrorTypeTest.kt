package com.chaddy50.concerttracker.data.external.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ApiErrorTypeTest {

    @Test
    fun `Type contains all expected entries`() {
        val names = ApiErrorType.Type.entries.map { it.name }.toSet()
        assertTrue(names, "NETWORK")
        assertTrue(names, "TIMEOUT")
        assertTrue(names, "SERVER")
        assertTrue(names, "CLIENT")
        assertTrue(names, "CONFLICT")
        assertTrue(names, "UNKNOWN")
    }

    @Test
    fun `Type entries all have distinct names`() {
        val names = ApiErrorType.Type.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `toUserMessage returns a non-blank string for every Type`() {
        ApiErrorType.Type.entries.forEach { type ->
            assertFalse("Expected non-blank message for $type", type.toUserMessage().isBlank())
        }
    }

    @Test
    fun `toUserMessage returns a distinct string for each Type`() {
        val messages = ApiErrorType.Type.entries.map { it.toUserMessage() }
        assertEquals(
            "Expected each Type to map to a unique message",
            messages.size,
            messages.toSet().size
        )
    }

    @Test
    fun `toUserMessage does not throw for any Type`() {
        ApiErrorType.Type.entries.forEach { it.toUserMessage() }
    }

    private fun assertTrue(set: Set<String>, value: String) {
        assert(set.contains(value)) { "Expected $value in $set" }
    }
}
