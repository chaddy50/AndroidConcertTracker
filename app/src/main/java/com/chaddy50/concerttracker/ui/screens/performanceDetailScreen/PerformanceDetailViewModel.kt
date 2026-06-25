package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
import com.chaddy50.concerttracker.data.api.SetListEntryRequest
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.navigation.routes.PerformanceDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
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

    var uiState: PerformanceDetailUiState by mutableStateOf(PerformanceDetailUiState.Loading)
        private set

    var draftNotes: Map<String, String> by mutableStateOf(emptyMap())
        private set

    var didSavingNotesHaveError: ApiErrorType.Type? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            snapshotFlow { draftNotes }
                .drop(1)
                .debounce(1000L.milliseconds)
                .collect { autoSaveNotes(it) }
        }
        loadPerformance()
    }

    fun loadPerformance() {
        viewModelScope.launch {
            uiState = PerformanceDetailUiState.Loading
            when (val result = performancesRepository.getPerformance(performanceId)) {
                is ApiResult.Success -> {
                    val performance = result.data
                    draftNotes = performance.setList.associate { it.id to (it.notes ?: "") }
                    uiState = PerformanceDetailUiState.Success(performance)
                }
                is ApiResult.Error -> uiState = PerformanceDetailUiState.Error(result.errorType)
            }
        }
    }

    fun updateDraftNote(entryId: String, notes: String) {
        draftNotes = draftNotes + (entryId to notes)
    }

    private fun autoSaveNotes(notes: Map<String, String>) {
        val performance = (uiState as? PerformanceDetailUiState.Success)?.performance ?: return
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
                    SetListEntryRequest(notes = note.ifBlank { null })
                )
                if (result is ApiResult.Error) {
                    anyFailed = true
                    didSavingNotesHaveError = result.errorType
                }
            }
            if (!anyFailed) {
                didSavingNotesHaveError = null
                val updatedSetList = performance.setList.map { entry ->
                    entry.copy(notes = notes[entry.id]?.ifBlank { null } ?: entry.notes)
                }
                uiState = PerformanceDetailUiState.Success(performance.copy(setList = updatedSetList))
            }
        }
    }
}

sealed interface PerformanceDetailUiState {
    data object Loading : PerformanceDetailUiState
    data class Success(val performance: Performance) : PerformanceDetailUiState
    data class Error(val errorType: ApiErrorType.Type) : PerformanceDetailUiState
}
