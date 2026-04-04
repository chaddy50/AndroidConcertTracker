package com.chaddy50.concerttracker.ui.musicBrainzSearch

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.entity.MusicBrainzResult
import com.chaddy50.concerttracker.data.repository.MusicBrainzRepository
import com.chaddy50.concerttracker.navigation.MusicBrainzSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicBrainzSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicBrainzRepository: MusicBrainzRepository
) : ViewModel() {

    private val entityType = savedStateHandle.toRoute<MusicBrainzSearch>().entityType

    var searchQuery: String by mutableStateOf("")
        private set

    var uiState: MusicBrainzSearchUiState by mutableStateOf(MusicBrainzSearchUiState.Idle)
        private set

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun search() {
        viewModelScope.launch {
            uiState = MusicBrainzSearchUiState.Loading
            try {
                val results = musicBrainzRepository.search(entityType, searchQuery)
                uiState = if (results.isEmpty()) {
                    MusicBrainzSearchUiState.Empty
                } else {
                    MusicBrainzSearchUiState.Results(results)
                }
            } catch (e: Exception) {
                Log.e("MusicBrainzSearch", "Search failed", e)
                uiState = MusicBrainzSearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface MusicBrainzSearchUiState {
    data object Idle : MusicBrainzSearchUiState
    data object Loading : MusicBrainzSearchUiState
    data object Empty : MusicBrainzSearchUiState
    data class Results(val results: List<MusicBrainzResult>) : MusicBrainzSearchUiState
    data class Error(val message: String) : MusicBrainzSearchUiState
}
