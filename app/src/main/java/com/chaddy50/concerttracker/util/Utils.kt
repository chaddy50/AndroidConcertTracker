package com.chaddy50.concerttracker.util

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.util.Date

fun formatDateTime(isoDate: String, context: Context): String {
    return try {
        val instant = Instant.parse(isoDate)
        val date = Date.from(instant)
        val dateFormatter = DateFormat.getMediumDateFormat(context)
        val timeFormatter = DateFormat.getTimeFormat(context)
        "${dateFormatter.format(date)}, ${timeFormatter.format(date)}"
    } catch (_: Exception) {
        isoDate
    }
}

fun formatDate(isoDate: String, context: Context): String {
    return try {
        val instant = Instant.parse(isoDate)
        val formatter = DateFormat.getMediumDateFormat(context)
        formatter.format(Date.from(instant))
    } catch (_: Exception) {
        isoDate
    }
}

fun formatTime(isoDate: String, context: Context): String {
    return try {
        val instant = Instant.parse(isoDate)
        val formatter = DateFormat.getTimeFormat(context)
        formatter.format(Date.from(instant))
    } catch (_: Exception) {
        isoDate
    }
}

fun isoToEpochMillis(isoDate: String): Long? = try {
    Instant.parse(isoDate).toEpochMilli()
} catch (_: Exception) {
    null
}

fun epochMillisToIso(millis: Long): String =
    Instant.ofEpochMilli(millis).toString()