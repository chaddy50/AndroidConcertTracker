package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.repository.ComposersRepository
import com.chaddy50.concerttracker.data.repository.OpenOpusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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

    fun updateSearchQuery(query: String) {
        searchQuery = query
        uiState = ComposerSearchUiState.Idle
    }

    fun search() {
        if (searchQuery.isBlank()) return
        viewModelScope.launch {
            uiState = ComposerSearchUiState.Loading
            val localDeferred = async { composersRepository.searchComposers(searchQuery.trim()) }
            val apiDeferred = async { openOpusRepository.searchComposers(searchQuery.trim()) }

            val localResult = localDeferred.await()
            val apiResult = apiDeferred.await()

            val localComposers = (localResult as? ApiResult.Success)?.data
            val apiComposers = (apiResult as? ApiResult.Success)?.data

            if (localComposers == null && apiComposers == null) {
                uiState = ComposerSearchUiState.Error((localResult as ApiResult.Error).errorType)
                return@launch
            }

            val apiOpenOpusIds = (apiComposers ?: emptyList()).map { it.id }.toSet()
            val localOnly = (localComposers ?: emptyList())
                .filter { it.openOpusId == null || it.openOpusId !in apiOpenOpusIds }
                .map { OpenOpusComposer(id = it.openOpusId ?: "", name = it.name, completeName = it.name) }

            val combined = localOnly + (apiComposers ?: emptyList())
            uiState = if (combined.isEmpty()) ComposerSearchUiState.Empty else ComposerSearchUiState.Results(combined)
        }
    }
}

sealed interface ComposerSearchUiState {
    data object Idle : ComposerSearchUiState
    data object Loading : ComposerSearchUiState
    data object Empty : ComposerSearchUiState
    data class Results(val composers: List<OpenOpusComposer>) : ComposerSearchUiState
    data class Error(val errorType: ApiErrorType.Type) : ComposerSearchUiState
}
