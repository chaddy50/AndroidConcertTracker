package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.OpenOpusApiService
import com.chaddy50.concerttracker.data.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.api.OpenOpusWork
import com.chaddy50.concerttracker.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenOpusRepository @Inject constructor(
    private val openOpusApiService: OpenOpusApiService
) {
    suspend fun searchComposers(query: String): ApiResult<List<OpenOpusComposer>> = safeApiCall {
        openOpusApiService.searchComposers(query).composers
    }

    suspend fun getWorksByComposer(composerId: String): ApiResult<List<OpenOpusWork>> = safeApiCall {
        openOpusApiService.getWorksByComposer(composerId).works.sortedBy { it.title }
    }
}
