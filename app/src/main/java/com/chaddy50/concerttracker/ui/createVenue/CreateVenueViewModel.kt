package com.chaddy50.concerttracker.ui.createVenue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.entity.NominatimResult
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.VenueRequest
import com.chaddy50.concerttracker.data.repository.NominatimRepository
import com.chaddy50.concerttracker.data.repository.VenuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateVenueViewModel @Inject constructor(
    private val nominatimRepository: NominatimRepository,
    private val venuesRepository: VenuesRepository
) : ViewModel() {

    var searchQuery: String by mutableStateOf("")
        private set

    var uiState: CreateVenueUiState by mutableStateOf(CreateVenueUiState.Idle)
        private set

    var isSaving: Boolean by mutableStateOf(false)
        private set

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun search() {
        viewModelScope.launch {
            uiState = CreateVenueUiState.Loading
            try {
                val results = nominatimRepository.searchVenues(searchQuery)
                uiState = if (results.isEmpty()) {
                    CreateVenueUiState.Empty
                } else {
                    CreateVenueUiState.Results(results)
                }
            } catch (e: Exception) {
                uiState = CreateVenueUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveVenue(result: NominatimResult, onSaved: (Venue) -> Unit) {
        viewModelScope.launch {
            isSaving = true
            try {
                val venue = venuesRepository.createVenue(
                    VenueRequest(
                        osmType = result.osmType,
                        osmId = result.osmId.toString(),
                        name = result.name
                    )
                )
                onSaved(venue)
            } catch (_: Exception) {
                // TODO: surface error to user
            } finally {
                isSaving = false
            }
        }
    }
}

sealed interface CreateVenueUiState {
    data object Idle : CreateVenueUiState
    data object Loading : CreateVenueUiState
    data object Empty : CreateVenueUiState
    data class Results(val results: List<NominatimResult>) : CreateVenueUiState
    data class Error(val message: String) : CreateVenueUiState
}