package com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.MusicBrainzResult
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.MusicBrainzRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.navigation.routes.MusicBrainzSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicBrainzSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicBrainzRepository: MusicBrainzRepository,
    private val performersRepository: PerformersRepository
) : ViewModel() {

    val entityType = savedStateHandle.toRoute<MusicBrainzSearch>().entityType

    var searchQuery: String by mutableStateOf("")
        private set

    var uiState: MusicBrainzSearchUiState by mutableStateOf(MusicBrainzSearchUiState.Idle)
        private set

    fun updateSearchQuery(query: String) {
        searchQuery = query
        uiState = MusicBrainzSearchUiState.Idle
    }

    fun search() {
        viewModelScope.launch {
            uiState = MusicBrainzSearchUiState.Loading

            val localDeferred = async { performersRepository.searchPerformers(searchQuery) }
            val apiDeferred = async { musicBrainzRepository.search(entityType, searchQuery) }

            val localResult = localDeferred.await()
            val apiResult = apiDeferred.await()

            val localPerformers = (localResult as? ApiResult.Success)?.data?.let(::buildListOfLocalPerformers)
            val apiPerformers = (apiResult as? ApiResult.Success)?.data

            if (localPerformers == null && apiPerformers == null) {
                uiState = MusicBrainzSearchUiState.Error((localResult as ApiResult.Error).errorType)
                return@launch
            }

            val localIds = (localPerformers ?: emptyList()).map { it.id }.toSet()
            val combined = (localPerformers ?: emptyList()) + (apiPerformers ?: emptyList()).filter { it.id !in localIds }
            uiState = if (combined.isEmpty()) MusicBrainzSearchUiState.Empty else MusicBrainzSearchUiState.Results(combined)
        }
    }

    private fun buildListOfLocalPerformers(performers: List<Performer>): List<MusicBrainzResult> {
        return performers
            .filter { performer ->
                when (entityType) {
                    MusicBrainzEntityType.PERFORMER -> performer.type != PerformerType.CONDUCTOR
                    MusicBrainzEntityType.CONDUCTOR -> performer.type == PerformerType.CONDUCTOR
                    MusicBrainzEntityType.COMPOSER -> false
                }
            }
            .map { performer ->
                MusicBrainzResult(
                    id = performer.musicbrainzId ?: "",
                    name = performer.name,
                    description = performer.specialty,
                    performerType = performer.type
                )
            }
    }
}

sealed interface MusicBrainzSearchUiState {
    data object Idle : MusicBrainzSearchUiState
    data object Loading : MusicBrainzSearchUiState
    data object Empty : MusicBrainzSearchUiState
    data class Results(val results: List<MusicBrainzResult>) : MusicBrainzSearchUiState
    data class Error(val errorType: ApiErrorType.Type) : MusicBrainzSearchUiState
}
