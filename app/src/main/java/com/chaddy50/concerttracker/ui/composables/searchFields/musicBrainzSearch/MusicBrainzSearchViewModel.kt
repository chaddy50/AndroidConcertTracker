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
import com.chaddy50.concerttracker.data.external.api.PerformerRequest
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.MusicBrainzRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.navigation.routes.MusicBrainzSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    // The merged list is (performers cached in Room) + (online MusicBrainz results), de-duplicated by
    // MusicBrainz id, presented as one list the user can't tell apart. Cached performers stay usable offline.
    private var localPerformers: List<Performer> = emptyList()
    private var apiPerformers: List<MusicBrainzResult> = emptyList()
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
            performersRepository.searchPerformers(query).collect { performers ->
                localPerformers = performers.filter(::doesEntityTypeMatch)
                recomputeUiState()
            }
        }
    }

    private fun doesEntityTypeMatch(performer: Performer): Boolean = when (entityType) {
        MusicBrainzEntityType.PERFORMER -> performer.type != PerformerType.CONDUCTOR
        MusicBrainzEntityType.CONDUCTOR -> performer.type == PerformerType.CONDUCTOR
        MusicBrainzEntityType.COMPOSER -> false
    }

    fun search() {
        viewModelScope.launch {
            isSearching = true
            hasSearched = true
            apiError = null
            recomputeUiState()
            when (val result = musicBrainzRepository.search(entityType, searchQuery)) {
                is ApiResult.Success -> apiPerformers = result.data
                is ApiResult.Error -> {
                    apiPerformers = emptyList()
                    apiError = result.errorType
                }
            }
            isSearching = false
            recomputeUiState()
        }
    }

    fun selectPerformer(performer: Performer, onSelected: (Performer) -> Unit) = onSelected(performer)

    fun selectPerformerFromApi(result: MusicBrainzResult, onSelected: (Performer) -> Unit) =
        findOrCreatePerformer(result.name, result.performerType ?: PerformerType.OTHER, result.description, result.id, onSelected)

    fun createCustomPerformer(name: String, type: PerformerType, specialty: String?, onSelected: (Performer) -> Unit) =
        findOrCreatePerformer(name, type, specialty, null, onSelected)

    private fun findOrCreatePerformer(
        name: String,
        type: PerformerType,
        specialty: String?,
        musicbrainzId: String?,
        onSelected: (Performer) -> Unit
    ) {
        viewModelScope.launch {
            isSaving = true
            saveError = null
            when (val result = performersRepository.findOrCreatePerformer(PerformerRequest(name, type, specialty, musicbrainzId))) {
                is ApiResult.Success -> onSelected(result.data)
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
            isSaving = false
        }
    }

    private fun recomputeUiState() {
        val rows = buildRows()
        uiState = when {
            rows.isNotEmpty() -> MusicBrainzSearchUiState.Results(rows)
            isSearching -> MusicBrainzSearchUiState.Loading
            apiError != null -> MusicBrainzSearchUiState.Error(apiError!!)
            hasSearched -> MusicBrainzSearchUiState.Empty
            else -> MusicBrainzSearchUiState.Idle
        }
    }

    private fun buildRows(): List<PerformerSearchResult> {
        val cachedMusicBrainzIds = localPerformers.mapNotNull { it.musicbrainzId }.toSet()
        val localRows = localPerformers.map { PerformerSearchResult.Local(it) }
        val apiRows = apiPerformers
            .filter { it.id !in cachedMusicBrainzIds }
            .map { PerformerSearchResult.FromApi(it) }
        return localRows + apiRows
    }
}

sealed interface MusicBrainzSearchUiState {
    data object Idle : MusicBrainzSearchUiState
    data object Loading : MusicBrainzSearchUiState
    data object Empty : MusicBrainzSearchUiState
    data class Results(val rows: List<PerformerSearchResult>) : MusicBrainzSearchUiState
    data class Error(val errorType: ApiErrorType.Type) : MusicBrainzSearchUiState
}
