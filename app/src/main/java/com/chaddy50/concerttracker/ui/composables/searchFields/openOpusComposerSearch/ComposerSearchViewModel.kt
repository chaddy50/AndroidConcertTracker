package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.repository.ComposersRepository
import com.chaddy50.concerttracker.data.repository.OpenOpusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComposerSearchViewModel @Inject constructor(
    private val openOpusRepository: OpenOpusRepository,
    private val composersRepository: ComposersRepository
) : ViewModel() {

    var searchQuery: String by mutableStateOf("")
        private set

    var uiState: ComposerSearchUiState by mutableStateOf(ComposerSearchUiState.Idle)
        private set

    // The merged list is (composers cached in Room) + (online Open Opus results), de-duplicated by
    // Open Opus id, presented as one list the user can't tell apart. Cached composers stay usable offline.
    private var localComposers: List<Composer> = emptyList()
    private var apiComposers: List<OpenOpusComposer> = emptyList()
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
            composersRepository.searchComposers(query).collect { composers ->
                localComposers = composers
                recomputeUiState()
            }
        }
    }

    fun search() {
        if (searchQuery.isBlank()) return
        viewModelScope.launch {
            isSearching = true
            hasSearched = true
            apiError = null
            recomputeUiState()
            when (val result = openOpusRepository.searchComposers(searchQuery.trim())) {
                is ApiResult.Success -> apiComposers = result.data
                is ApiResult.Error -> {
                    apiComposers = emptyList()
                    apiError = result.errorType
                }
            }
            isSearching = false
            recomputeUiState()
        }
    }

    private fun recomputeUiState() {
        val rows = buildRows()
        uiState = when {
            rows.isNotEmpty() -> ComposerSearchUiState.Results(rows)
            isSearching -> ComposerSearchUiState.Loading
            apiError != null -> ComposerSearchUiState.Error(apiError!!)
            hasSearched -> ComposerSearchUiState.Empty
            else -> ComposerSearchUiState.Idle
        }
    }

    private fun buildRows(): List<ComposerSearchResult> {
        val cachedOpenOpusIds = localComposers.mapNotNull { it.openOpusId }.toSet()
        val localRows = localComposers.map { ComposerSearchResult.Local(it) }
        val apiRows = apiComposers
            .filter { it.id !in cachedOpenOpusIds }
            .map { ComposerSearchResult.FromApi(it) }
        return localRows + apiRows
    }
}

sealed interface ComposerSearchUiState {
    data object Idle : ComposerSearchUiState
    data object Loading : ComposerSearchUiState
    data object Empty : ComposerSearchUiState
    data class Results(val rows: List<ComposerSearchResult>) : ComposerSearchUiState
    data class Error(val errorType: ApiErrorType.Type) : ComposerSearchUiState
}
