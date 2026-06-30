package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.NominatimApiService
import com.chaddy50.concerttracker.data.external.api.NominatimResult
import com.chaddy50.concerttracker.data.external.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NominatimRepository @Inject constructor(
    private val nominatimApiService: NominatimApiService
) {
    suspend fun searchVenues(query: String): ApiResult<List<NominatimResult>> = safeApiCall {
        nominatimApiService.search(query).filter { it.name.isNotBlank() }
    }
}