package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.api.OpenOpusApiService
import com.chaddy50.concerttracker.data.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.repository.ComposersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComposerSearchViewModel @Inject constructor(
    private val openOpusApiService: OpenOpusApiService,
    private val composersRepository: ComposersRepository
) : ViewModel() {

    var searchQuery: String by mutableStateOf("")
        private set

    var uiState: ComposerSearchUiState by mutableStateOf(ComposerSearchUiState.Idle)
        private set

    fun updateSearchQuery(query: String) {
        searchQuery = query
        uiState = ComposerSearchUiState.Idle
    }

    fun search() {
        if (searchQuery.isBlank()) return
        viewModelScope.launch {
            uiState = ComposerSearchUiState.Loading
            try {
                coroutineScope {
                    val localDeferred = async {
                        runCatching {
                            composersRepository.searchComposers(searchQuery.trim())
                        }.getOrDefault(emptyList())
                    }
                    val apiDeferred = async {
                        runCatching {
                            openOpusApiService.searchComposers(searchQuery.trim()).composers
                        }.getOrDefault(emptyList())
                    }

                    val localComposers = localDeferred.await()
                    val apiComposers = apiDeferred.await()

                    val apiOpenOpusIds = apiComposers.map { it.id }.toSet()
                    val localOnly = localComposers
                        .filter { it.openOpusId == null || it.openOpusId !in apiOpenOpusIds }
                        .map { OpenOpusComposer(id = it.openOpusId ?: "", name = it.name, completeName = it.name) }

                    val combined = localOnly + apiComposers
                    uiState = if (combined.isEmpty()) {
                        ComposerSearchUiState.Empty
                    } else {
                        ComposerSearchUiState.Results(combined)
                    }
                }
            } catch (exception: Exception) {
                uiState = ComposerSearchUiState.Error(exception.message ?: "Unknown error")
            }
        }
    }
}

sealed interface ComposerSearchUiState {
    data object Idle : ComposerSearchUiState
    data object Loading : ComposerSearchUiState
    data object Empty : ComposerSearchUiState
    data class Results(val composers: List<OpenOpusComposer>) : ComposerSearchUiState
    data class Error(val message: String) : ComposerSearchUiState
}