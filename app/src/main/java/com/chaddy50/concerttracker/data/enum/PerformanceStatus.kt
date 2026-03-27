package com.chaddy50.concerttracker.data.enum

import kotlinx.serialization.Serializable

@Serializable
enum class PerformanceStatus {
    ATTENDED, UPCOMING, CANCELLED
}
