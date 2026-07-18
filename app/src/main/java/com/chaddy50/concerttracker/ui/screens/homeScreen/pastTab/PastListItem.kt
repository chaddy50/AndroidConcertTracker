package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.util.getYearFromIsoDateTime
import java.time.ZoneId

sealed interface PastListItem {
    data class Header(val yearLabel: String) : PastListItem
    data class Entry(val performance: Performance) : PastListItem
}

/**
 * Separator to place *between* [before] and [after], for use with `PagingData.insertSeparators`.
 * Returns a [PastListItem.Header] when [after] opens a new calendar year, or null when [after] is in the same year as [before].
 */
fun pastListSeparator(
    before: PastListItem.Entry?,
    after: PastListItem.Entry?,
    zone: ZoneId = ZoneId.systemDefault()
): PastListItem.Header? {
    if (after == null) return null
    val afterYear = getYearFromIsoDateTime(after.performance.date, zone)

    if (before == null) return PastListItem.Header(afterYear)

    return if (getYearFromIsoDateTime(before.performance.date, zone) != afterYear) {
        PastListItem.Header(afterYear)
    } else {
        null
    }
}
