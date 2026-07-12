package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.navigation.routes.PerformanceDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class PerformanceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val performancesRepository: PerformancesRepository,
    private val setListEntriesRepository: SetListEntriesRepository
) : ViewModel() {

    private val performanceId: String = savedStateHandle.toRoute<PerformanceDetail>().id

    private val isLoading = MutableStateFlow(true)

    /** The latest cached performance, kept in sync with Room so note auto-save can diff against it. */
    private var loadedPerformance: Performance? = null

    val uiState: StateFlow<PerformanceDetailUiState> = combine(
        performancesRepository.observePerformance(performanceId),
        isLoading
    ) { performance, loading ->
        // Offline-first: Room is the source of truth. A missing performance simply means Empty
        // ("not found").
        when {
            performance != null -> PerformanceDetailUiState.Content(performance)
            loading -> PerformanceDetailUiState.Loading
            else -> PerformanceDetailUiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PerformanceDetailUiState.Loading
    )

    var draftNotes: Map<String, String> by mutableStateOf(emptyMap())
        private set

    var didSavingNotesHaveError: ApiErrorType.Type? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            performancesRepository.observePerformance(performanceId).collect { performance ->
                loadedPerformance = performance
                if (performance != null) seedDraftNotes(performance)
                isLoading.value = false
            }
        }
        viewModelScope.launch {
            snapshotFlow { draftNotes }
                .drop(1)
                .debounce(1000L.milliseconds)
                .collect { autoSaveNotes(it) }
        }
    }

    fun updateDraftNote(entryId: String, notes: String) {
        draftNotes = draftNotes + (entryId to notes)
    }

    /**
     * Seeds note drafts for set-list entries we haven't seen yet, preserving any in-progress edits
     * and dropping drafts for entries that no longer exist. Room re-emits on every write-through, so
     * we must not clobber what the user is currently typing.
     */
    private fun seedDraftNotes(performance: Performance) {
        val validIds = performance.setList.map { it.id }.toSet()
        val seeded = draftNotes.toMutableMap()
        performance.setList.forEach { entry ->
            if (!seeded.containsKey(entry.id)) seeded[entry.id] = entry.notes ?: ""
        }
        draftNotes = seeded.filterKeys { it in validIds }
    }

    private fun autoSaveNotes(notes: Map<String, String>) {
        val performance = loadedPerformance ?: return
        viewModelScope.launch {
            val changedNotes = notes.filter { (entryId, note) ->
                val original = performance.setList.find { it.id == entryId }?.notes ?: ""
                note != original
            }
            if (changedNotes.isEmpty()) return@launch
            var anyFailed = false
            changedNotes.forEach { (entryId, note) ->
                val result = setListEntriesRepository.updateSetListEntry(
                    entryId,
                    note.ifBlank { null }
                )
                if (result is ApiResult.Error) {
                    anyFailed = true
                    didSavingNotesHaveError = result.errorType
                }
            }
            if (!anyFailed) {
                didSavingNotesHaveError = null
            }
        }
    }
}

sealed interface PerformanceDetailUiState {
    data object Loading : PerformanceDetailUiState
    data object Empty : PerformanceDetailUiState
    data class Content(val performance: Performance) : PerformanceDetailUiState
}
