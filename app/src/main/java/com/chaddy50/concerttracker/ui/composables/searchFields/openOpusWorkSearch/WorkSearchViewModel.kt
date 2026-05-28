package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.api.OpenOpusApiService
import com.chaddy50.concerttracker.data.api.OpenOpusWork
import com.chaddy50.concerttracker.navigation.routes.OpenOpusWorkSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OpenOpusGenre(val displayName: String) {
    ALL("All"),
    ORCHESTRAL("Orchestral"),
    VOCAL("Vocal"),
    KEYBOARD("Keyboard"),
    CHAMBER("Chamber"),
    STAGE("Stage")
}

@HiltViewModel
class WorkSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val openOpusApiService: OpenOpusApiService
) : ViewModel() {

    private val route = savedStateHandle.toRoute<OpenOpusWorkSearch>()
    val composerId: String = route.composerId
    val composerCompleteName: String = route.composerCompleteName

    var searchQuery: String by mutableStateOf("")
        private set

    var selectedGenre: OpenOpusGenre by mutableStateOf(OpenOpusGenre.ALL)
        private set

    var uiState: WorkSearchUiState by mutableStateOf(WorkSearchUiState.Idle)
        private set

    val filteredWorks: List<OpenOpusWork>
        get() {
            val works = (uiState as? WorkSearchUiState.Results)?.works ?: return emptyList()
            return works.filter { work ->
                val genreMatches = selectedGenre == OpenOpusGenre.ALL ||
                    work.genre?.lowercase() == selectedGenre.name.lowercase()
                val queryMatches = searchQuery.isBlank() ||
                    work.title.contains(searchQuery.trim(), ignoreCase = true)
                genreMatches && queryMatches
            }
        }

    init {
        if (composerId.isNotBlank()) {
            fetchWorks()
        }
    }

    private fun fetchWorks() {
        viewModelScope.launch {
            uiState = WorkSearchUiState.Loading
            try {
                val works = openOpusApiService.getWorksByComposer(composerId).works.sortedBy { it.title }
                uiState = WorkSearchUiState.Results(works)
            } catch (exception: Exception) {
                uiState = WorkSearchUiState.Error(exception.message ?: "Unknown error")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun selectGenre(genre: OpenOpusGenre) {
        selectedGenre = genre
    }
}

sealed interface WorkSearchUiState {
    data object Idle : WorkSearchUiState
    data object Loading : WorkSearchUiState
    data class Results(val works: List<OpenOpusWork>) : WorkSearchUiState
    data class Error(val message: String) : WorkSearchUiState
}