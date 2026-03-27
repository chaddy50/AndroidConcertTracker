package com.chaddy50.concerttracker.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
        formatter.format(localDate)
    } catch (_: Exception) {
        isoDate
    }
}