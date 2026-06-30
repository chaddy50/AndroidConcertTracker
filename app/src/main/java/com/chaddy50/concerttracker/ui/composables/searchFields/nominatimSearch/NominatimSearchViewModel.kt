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

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun search() {
        viewModelScope.launch {
            uiState = CreateVenueUiState.Loading
            uiState = when (val result = nominatimRepository.searchVenues(searchQuery)) {
                is ApiResult.Success -> if (result.data.isEmpty()) {
                    CreateVenueUiState.Empty
                } else {
                    CreateVenueUiState.Results(result.data)
                }
                is ApiResult.Error -> CreateVenueUiState.Error(result.errorType)
            }
        }
    }

    fun saveVenue(result: NominatimResult, onSaved: (Venue) -> Unit) {
        viewModelScope.launch {
            isSaving = true
            saveError = null
            when (val apiResult = venuesRepository.createVenue(
                VenueRequest(osmType = result.osmType, osmId = result.osmId.toString(), name = result.name)
            )) {
                is ApiResult.Success -> onSaved(apiResult.data)
                is ApiResult.Error -> saveError = apiResult.errorType.toUserMessage()
            }
            isSaving = false
        }
    }
}

sealed interface CreateVenueUiState {
    data object Idle : CreateVenueUiState
    data object Loading : CreateVenueUiState
    data object Empty : CreateVenueUiState
    data class Results(val results: List<NominatimResult>) : CreateVenueUiState
    data class Error(val errorType: ApiErrorType.Type) : CreateVenueUiState
}
