package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.NominatimApiService
import com.chaddy50.concerttracker.data.entity.NominatimResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NominatimRepository @Inject constructor(
    private val nominatimApiService: NominatimApiService
) {
    suspend fun searchVenues(query: String): List<NominatimResult> {
        return nominatimApiService.search(query).filter { it.name.isNotBlank() }
    }
}