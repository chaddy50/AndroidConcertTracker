package com.chaddy50.concerttracker

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * Mirrors the production `Json` configured in `NetworkModule.provideJson()` so that
 * repository/serialization tests exercise the exact snake_case naming behavior used at runtime.
 */
@OptIn(ExperimentalSerializationApi::class)
fun testJson(): Json = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}
