package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.OpenOpusWork
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.repository.OpenOpusRepository
import com.chaddy50.concerttracker.data.repository.WorksRepository
import com.chaddy50.concerttracker.navigation.routes.OpenOpusWorkSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val openOpusRepository: OpenOpusRepository,
    private val worksRepository: WorksRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<OpenOpusWorkSearch>()
    private val composerEntityId: String? = route.composerEntityId
    private val composerOpenOpusId: String? = route.composerOpenOpusId
    val composerName: String = route.composerName

    var searchQuery: String by mutableStateOf("")
        private set

    var selectedGenre: OpenOpusGenre by mutableStateOf(OpenOpusGenre.ALL)
        private set

    var uiState: WorkSearchUiState by mutableStateOf(WorkSearchUiState.Idle)
        private set

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    // The merged list is (works cached in Room for this composer) + (online Open Opus works),
    // de-duplicated by Open Opus id, presented as one list. Cached works stay usable offline.
    private var localWorks: List<Work> = emptyList()
    private var apiWorks: List<OpenOpusWork> = emptyList()
    private var apiError: ApiErrorType.Type? = null
    private var isFetching = false
    private var hasFetched = false
    private var cachedJob: Job? = null

    init {
        if (composerEntityId != null) {
            observeCached("")
        }
        if (composerOpenOpusId != null) {
            fetchWorks()
        }
    }

    private fun observeCached(query: String) {
        val composerId = composerEntityId ?: return
        cachedJob?.cancel()
        cachedJob = viewModelScope.launch {
            worksRepository.searchWorksForComposer(composerId, query).collect { works ->
                localWorks = works
                recomputeUiState()
            }
        }
    }

    private fun fetchWorks() {
        val openOpusId = composerOpenOpusId ?: return
        viewModelScope.launch {
            isFetching = true
            hasFetched = true
            apiError = null
            recomputeUiState()
            when (val result = openOpusRepository.getWorksByComposer(openOpusId)) {
                is ApiResult.Success -> apiWorks = result.data
                is ApiResult.Error -> {
                    apiWorks = emptyList()
                    apiError = result.errorType
                }
            }
            isFetching = false
            recomputeUiState()
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        observeCached(query)
        recomputeUiState()
    }

    fun selectGenre(genre: OpenOpusGenre) {
        selectedGenre = genre
        recomputeUiState()
    }

    fun selectWork(work: Work, onSelected: (Work) -> Unit) = onSelected(work)

    fun selectWorkFromApi(work: OpenOpusWork, onSelected: (Work) -> Unit) =
        findOrCreateWork(openOpusWorkId = work.id, title = work.title, onSelected = onSelected)

    fun createCustomWork(title: String, onSelected: (Work) -> Unit) =
        findOrCreateWork(openOpusWorkId = null, title = title, onSelected = onSelected)

    private fun findOrCreateWork(openOpusWorkId: String?, title: String, onSelected: (Work) -> Unit) {
        viewModelScope.launch {
            isSaving = true
            saveError = null
            val result = worksRepository.findOrCreateWork(
                openOpusWorkId = openOpusWorkId,
                title = title,
                existingComposerId = composerEntityId,
                openOpusComposerId = composerOpenOpusId,
                composerName = composerName
            )
            when (result) {
                is ApiResult.Success -> onSelected(result.data)
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
            isSaving = false
        }
    }

    private fun recomputeUiState() {
        val rows = buildRows()
        uiState = when {
            rows.isNotEmpty() -> WorkSearchUiState.Results(rows)
            isFetching -> WorkSearchUiState.Loading
            apiError != null -> WorkSearchUiState.Error(apiError!!)
            hasFetched || composerEntityId != null -> WorkSearchUiState.Empty
            else -> WorkSearchUiState.Idle
        }
    }

    private fun buildRows(): List<WorkSearchResult> {
        // Cached works are already title-filtered by the DAO; catalog works filter by genre + query here.
        val cachedOpenOpusIds = localWorks.mapNotNull { it.openOpusId }.toSet()
        val localRows = localWorks.map { WorkSearchResult.Local(it) }
        val apiRows = apiWorks
            .filter { work ->
                work.id !in cachedOpenOpusIds &&
                    doesGenreMatch(work) &&
                    doesQueryMatch(work)
            }
            .map { WorkSearchResult.FromApi(it) }
        return localRows + apiRows
    }

    private fun doesGenreMatch(work: OpenOpusWork): Boolean =
        selectedGenre == OpenOpusGenre.ALL || work.genre?.lowercase() == selectedGenre.name.lowercase()

    private fun doesQueryMatch(work: OpenOpusWork): Boolean =
        searchQuery.isBlank() || work.title.contains(searchQuery.trim(), ignoreCase = true)
}

sealed interface WorkSearchUiState {
    data object Idle : WorkSearchUiState
    data object Loading : WorkSearchUiState
    data object Empty : WorkSearchUiState
    data class Results(val rows: List<WorkSearchResult>) : WorkSearchUiState
    data class Error(val errorType: ApiErrorType.Type) : WorkSearchUiState
}
