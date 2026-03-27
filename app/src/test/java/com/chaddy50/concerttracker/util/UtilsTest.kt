package com.chaddy50.concerttracker.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class UtilsTest {

    @Test
    fun `formatDate formats a valid ISO date string`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("15 November 2024", formatDate("2024-11-15T19:30:00.000Z"))
    }

    @Test
    fun `formatDate returns the original string when the input is not a valid ISO date`() {
        assertEquals("not-a-date", formatDate("not-a-date"))
    }

    @Test
    fun `formatDate returns the original string when the input is empty`() {
        assertEquals("", formatDate(""))
    }
}