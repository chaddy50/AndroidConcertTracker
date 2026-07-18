package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class PastListItemTest {

    private val utc = ZoneId.of("UTC")

    private fun entry(date: String) = PastListItem.Entry(
        Performance(
            id = date,
            date = date,
            venue = Venue("v1", "Hall", "o", "way"),
            status = PerformanceStatus.ATTENDED
        )
    )

    private fun sep(before: String?, after: String?, zone: ZoneId = utc) =
        pastListSeparator(before?.let(::entry), after?.let(::entry), zone)

    // region first / last item

    @Test
    fun `first newest item emits its year header`() {
        assertEquals(PastListItem.Header("2024"), sep(null, "2024-11-05T12:00:00Z"))
    }

    @Test
    fun `past the oldest item emits no header`() {
        assertNull(sep("2024-11-05T12:00:00Z", null))
    }

    @Test
    fun `empty list edge emits no header`() {
        assertNull(sep(null, null))
    }

    // endregion

    // region year boundary

    @Test
    fun `same year emits no header even across a month change`() {
        assertNull(sep("2024-11-05T12:00:00Z", "2024-01-28T12:00:00Z"))
    }

    @Test
    fun `same exact date emits no header`() {
        assertNull(sep("2024-11-20T12:00:00Z", "2024-11-20T12:00:00Z"))
    }

    @Test
    fun `crossing a year boundary emits the older year's header`() {
        assertEquals(
            PastListItem.Header("2023"),
            sep("2024-01-10T12:00:00Z", "2023-12-20T12:00:00Z")
        )
    }

    @Test
    fun `multi-year gap uses the immediate neighbor's year`() {
        assertEquals(
            PastListItem.Header("2022"),
            sep("2024-05-10T12:00:00Z", "2022-03-20T12:00:00Z")
        )
    }

    // endregion

    // region timezone determinism

    @Test
    fun `same instants bucket into different years under different zones`() {
        val newer = "2025-01-01T02:00:00Z" // Jan 1 2025 UTC / Dec 31 2024 in LA
        val older = "2024-12-31T20:00:00Z" // Dec 31 2024 in both

        // In UTC the two are different years -> header for 2024.
        assertEquals(PastListItem.Header("2024"), sep(newer, older, utc))
        // In Los Angeles both are 2024 -> no header.
        assertNull(sep(newer, older, ZoneId.of("America/Los_Angeles")))
    }

    // endregion
}
