package com.chaddy50.concerttracker.ui.composables.searchFields.nominatimSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.NominatimResult
import com.chaddy50.concerttracker.data.external.api.VenueRequest
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.repository.NominatimRepository
import com.chaddy50.concerttracker.data.repository.VenuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NominatimSearchViewModel @Inject constructor(
    private val nominatimRepository: NominatimRepository,
    private val venuesRepository: VenuesRepository
) : ViewModel() {

    var searchQuery: String by mutableStateOf("")
        private set

    var uiState: CreateVenueUiState by mutableStateOf(CreateVenueUiState.Idle)
        private set

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    // The merged list is (venues cached in Room) + (online Nominatim results), de-duplicated by
    // osm identity, presented as one list the user can't tell apart. Cached venues stay usable offline.
    private var localVenues: List<Venue> = emptyList()
    private var apiVenues: List<NominatimResult> = emptyList()
    private var apiError: ApiErrorType.Type? = null
    private var isSearching = false
    private var hasSearched = false
    private var cachedJob: Job? = null

    init {
        observeCached("")
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        observeCached(query)
    }

    private fun observeCached(query: String) {
        cachedJob?.cancel()
        cachedJob = viewModelScope.launch {
            venuesRepository.searchVenues(query).collect { venues ->
                localVenues = venues
                recomputeUiState()
            }
        }
    }

    fun search() {
        viewModelScope.launch {
            isSearching = true
            hasSearched = true
            apiError = null
            recomputeUiState()
            when (val result = nominatimRepository.searchVenues(searchQuery)) {
                is ApiResult.Success -> apiVenues = result.data
                is ApiResult.Error -> {
                    apiVenues = emptyList()
                    apiError = result.errorType
                }
            }
            isSearching = false
            recomputeUiState()
        }
    }

    fun selectVenue(venue: Venue, onSaved: (Venue) -> Unit) = onSaved(venue)

    fun selectVenueFromApi(result: NominatimResult, onSaved: (Venue) -> Unit) {
        viewModelScope.launch {
            isSaving = true
            saveError = null
            when (val apiResult = venuesRepository.findOrCreateVenue(
                VenueRequest(
                    osmType = result.osmType,
                    osmId = result.osmId.toString(),
                    name = result.name,
                    formattedAddress = result.displayName,
                    city = result.address?.let { it.city ?: it.town ?: it.village },
                    country = result.address?.country
                )
            )) {
                is ApiResult.Success -> onSaved(apiResult.data)
                is ApiResult.Error -> saveError = apiResult.errorType.toUserMessage()
            }
            isSaving = false
        }
    }

    private fun recomputeUiState() {
        val rows = buildRows()
        uiState = when {
            rows.isNotEmpty() -> CreateVenueUiState.Results(rows)
            isSearching -> CreateVenueUiState.Loading
            apiError != null -> CreateVenueUiState.Error(apiError!!)
            hasSearched -> CreateVenueUiState.Empty
            else -> CreateVenueUiState.Idle
        }
    }

    private fun buildRows(): List<VenueSearchResult> {
        val cachedKeys = localVenues.map { it.osmType to it.osmId }.toSet()
        val localRows = localVenues.map { VenueSearchResult.Local(it) }
        val apiRows = apiVenues
            .filter { (it.osmType to it.osmId.toString()) !in cachedKeys }
            .map { VenueSearchResult.FromApi(it) }
        return localRows + apiRows
    }
}

sealed interface CreateVenueUiState {
    data object Idle : CreateVenueUiState
    data object Loading : CreateVenueUiState
    data object Empty : CreateVenueUiState
    data class Results(val rows: List<VenueSearchResult>) : CreateVenueUiState
    data class Error(val errorType: ApiErrorType.Type) : CreateVenueUiState
}
