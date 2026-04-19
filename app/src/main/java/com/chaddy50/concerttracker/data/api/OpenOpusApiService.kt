package com.chaddy50.concerttracker.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenOpusApiService {
    @GET("composer/list/search/{query}.json")
    suspend fun searchComposers(
        @Path("query") query: String
    ): OpenOpusComposerResponse

    @GET("work/list/composer/{composerId}/genre/all.json")
    suspend fun getWorksByComposer(
        @Path("composerId") composerId: String
    ): OpenOpusWorkResponse
}

@Serializable
data class OpenOpusComposerResponse(
    val status: OpenOpusStatus,
    val composers: List<OpenOpusComposer> = emptyList()
)

@Serializable
data class OpenOpusWorkResponse(
    val status: OpenOpusStatus,
    val composer: OpenOpusComposer? = null,
    val works: List<OpenOpusWork> = emptyList()
)

@Serializable
data class OpenOpusStatus(
    val success: String,
    val rows: Int = 0
)

@Serializable
data class OpenOpusComposer(
    val id: String,
    val name: String,
    @SerialName("complete_name") val completeName: String,
    val epoch: String? = null
)

@Serializable
data class OpenOpusWork(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val genre: String? = null
)