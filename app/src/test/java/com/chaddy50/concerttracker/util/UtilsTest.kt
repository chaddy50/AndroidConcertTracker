package com.chaddy50.concerttracker.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UtilsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

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
}