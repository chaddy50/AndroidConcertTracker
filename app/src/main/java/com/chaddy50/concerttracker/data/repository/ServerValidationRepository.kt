package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.ConcertTrackerApiService
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates that a candidate server URL actually points to a reachable Concert Tracker
 * server before it is persisted. Unlike [PerformancesRepository], the Retrofit built here
 * is one-shot (no caching) — each call validates the exact URL passed to it.
 */
@Singleton
class ServerValidationRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    /**
     * Confirms the host both responds and speaks the Concert Tracker API via a cheap
     * `GET /v1/performances?limit=1`. Never throws — a malformed URL or any transport
     * failure is caught by [safeApiCall] and returned as an [ApiResult.Error].
     */
    suspend fun validate(url: String): ApiResult<Unit> = safeApiCall {
        val baseUrl = "${url.trim().trimEnd('/')}/${ConcertTrackerApiService.API_VERSION}/"
        val service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ConcertTrackerApiService::class.java)
        service.getPerformances(limit = 1)
        Unit
    }
}

fun String.isValidServerUrlFormat(): Boolean = this.toHttpUrlOrNull() != null

enum class ServerUrlValidationError { INVALID_FORMAT, UNREACHABLE, SERVER_ERROR, UNKNOWN }

fun ApiErrorType.Type.toServerUrlValidationError(): ServerUrlValidationError = when (this) {
    ApiErrorType.Type.NETWORK, ApiErrorType.Type.TIMEOUT -> ServerUrlValidationError.UNREACHABLE
    ApiErrorType.Type.SERVER, ApiErrorType.Type.CLIENT -> ServerUrlValidationError.SERVER_ERROR
    ApiErrorType.Type.CONFLICT, ApiErrorType.Type.UNKNOWN -> ServerUrlValidationError.UNKNOWN
}
