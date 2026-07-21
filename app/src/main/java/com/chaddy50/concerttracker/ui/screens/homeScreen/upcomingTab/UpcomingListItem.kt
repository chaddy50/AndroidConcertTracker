package com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab

import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.util.getYearFromIsoDateTime
import java.time.ZoneId

sealed interface UpcomingListItem {
    data class Header(val yearLabel: String) : UpcomingListItem
    data class Entry(val performance: Performance) : UpcomingListItem
}

fun buildUpcomingListItems(
    performances: List<Performance>,
    zone: ZoneId = ZoneId.systemDefault()
): List<UpcomingListItem> = buildList {
    var currentYear: String? = null
    for (performance in performances) {
        val year = getYearFromIsoDateTime(performance.date, zone)
        if (year != currentYear) {
            add(UpcomingListItem.Header(year))
            currentYear = year
        }
        add(UpcomingListItem.Entry(performance))
    }
}
