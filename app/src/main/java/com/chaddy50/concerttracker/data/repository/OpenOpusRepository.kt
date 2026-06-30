package com.chaddy50.concerttracker.data.repository

import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusApiService
import com.chaddy50.concerttracker.data.external.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.external.api.OpenOpusWork
import com.chaddy50.concerttracker.data.external.api.safeApiCall
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
