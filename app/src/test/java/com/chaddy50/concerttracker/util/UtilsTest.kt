package com.chaddy50.concerttracker.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UtilsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val utc = ZoneId.of("UTC")
    private val la = ZoneId.of("America/Los_Angeles")

    @Test
    fun `formatDate formats a valid ISO date string`() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("Nov 15, 2024", formatDate("2024-11-15T19:30:00.000Z", context))
    }

    @Test
    fun `formatDate returns the original string when the input is not a valid ISO date`() {
        assertEquals("not-a-date", formatDate("not-a-date", context))
    }

    @Test
    fun `formatDate returns the original string when the input is empty`() {
        assertEquals("", formatDate("", context))
    }

    @Test
    fun `getYearFromIsoDateTime returns the calendar year for a mid-year instant`() {
        assertEquals("2024", getYearFromIsoDateTime("2024-06-15T12:00:00Z", utc))
    }

    @Test
    fun `getYearFromIsoDateTime uses the supplied zone at a year boundary`() {
        // 2025-01-01T04:00Z is still Dec 31 2024 in Los Angeles.
        assertEquals("2024", getYearFromIsoDateTime("2025-01-01T04:00:00Z", la))
    }

    @Test
    fun `getYearFromIsoDateTime handles a non-Z offset input`() {
        assertEquals("2024", getYearFromIsoDateTime("2024-03-10T23:30:00-05:00", utc))
    }

    @Test
    fun `getYearFromIsoDateTime handles a leap-day instant`() {
        assertEquals("2024", getYearFromIsoDateTime("2024-02-29T10:00:00Z", utc))
    }

    @Test(expected = DateTimeParseException::class)
    fun `getYearFromIsoDateTime throws on a malformed ISO string`() {
        getYearFromIsoDateTime("not-a-date", utc)
    }

    @Test(expected = DateTimeParseException::class)
    fun `getYearFromIsoDateTime throws on an empty string`() {
        getYearFromIsoDateTime("", utc)
    }
}