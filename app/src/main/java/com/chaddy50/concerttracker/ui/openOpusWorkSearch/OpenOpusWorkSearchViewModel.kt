package com.chaddy50.concerttracker.ui.openOpusWorkSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.api.OpenOpusApiService
import com.chaddy50.concerttracker.data.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.api.OpenOpusWork
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
class OpenOpusWorkSearchViewModel @Inject constructor(
    private val openOpusApiService: OpenOpusApiService
) : ViewModel() {

    var composerSearchQuery: String by mutableStateOf("")
        private set

    var workSearchQuery: String by mutableStateOf("")
        private set

    var uiState: OpenOpusWorkSearchUiState by mutableStateOf(
        OpenOpusWorkSearchUiState.ComposerSearch()
    )
        private set

    val filteredWorks: List<OpenOpusWork>
        get() {
            val state = uiState as? OpenOpusWorkSearchUiState.WorkList ?: return emptyList()
            return state.allWorks.filter { work ->
                val genreMatches = state.selectedGenre == OpenOpusGenre.ALL ||
                    work.genre?.lowercase() == state.selectedGenre.name.lowercase()
                val queryMatches = workSearchQuery.isBlank() ||
                    work.title.contains(workSearchQuery.trim(), ignoreCase = true)
                genreMatches && queryMatches
            }
        }

    fun updateComposerSearchQuery(query: String) {
        composerSearchQuery = query
    }

    fun updateWorkSearchQuery(query: String) {
        workSearchQuery = query
    }

    fun searchComposers() {
        if (composerSearchQuery.isBlank()) return
        viewModelScope.launch {
            uiState = OpenOpusWorkSearchUiState.ComposerSearch(isLoading = true)
            try {
                val response = openOpusApiService.searchComposers(composerSearchQuery.trim())
                uiState = OpenOpusWorkSearchUiState.ComposerSearch(
                    composers = response.composers
                )
            } catch (exception: Exception) {
                uiState = OpenOpusWorkSearchUiState.ComposerSearch(
                    error = exception.message ?: "Unknown error"
                )
            }
        }
    }

    fun selectComposer(composer: OpenOpusComposer) {
        viewModelScope.launch {
            uiState = OpenOpusWorkSearchUiState.WorkList(
                composer = composer,
                isLoading = true
            )
            try {
                val response = openOpusApiService.getWorksByComposer(composer.id)
                uiState = OpenOpusWorkSearchUiState.WorkList(
                    composer = composer,
                    allWorks = response.works.sortedBy { it.title }
                )
            } catch (exception: Exception) {
                uiState = OpenOpusWorkSearchUiState.WorkList(
                    composer = composer,
                    error = exception.message ?: "Unknown error"
                )
            }
        }
    }

    fun selectGenre(genre: OpenOpusGenre) {
        val state = uiState as? OpenOpusWorkSearchUiState.WorkList ?: return
        uiState = state.copy(selectedGenre = genre)
    }

    fun backToComposerSearch() {
        workSearchQuery = ""
        uiState = OpenOpusWorkSearchUiState.ComposerSearch(
            composers = if (uiState is OpenOpusWorkSearchUiState.ComposerSearch) {
                (uiState as OpenOpusWorkSearchUiState.ComposerSearch).composers
            } else {
                emptyList()
            }
        )
    }
}

sealed interface OpenOpusWorkSearchUiState {
    data class ComposerSearch(
        val isLoading: Boolean = false,
        val composers: List<OpenOpusComposer> = emptyList(),
        val error: String? = null
    ) : OpenOpusWorkSearchUiState

    data class WorkList(
        val composer: OpenOpusComposer,
        val isLoading: Boolean = false,
        val allWorks: List<OpenOpusWork> = emptyList(),
        val selectedGenre: OpenOpusGenre = OpenOpusGenre.ALL,
        val error: String? = null
    ) : OpenOpusWorkSearchUiState
}
