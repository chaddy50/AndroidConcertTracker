package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.SetListEntryRequest
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.navigation.PerformanceDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    init {
        viewModelScope.launch {
            performancesRepository.performances
                .drop(1)
                .mapNotNull { list -> list.find { it.id == performanceId } }
                .collect { performance ->
                    draftNotes = performance.setList.associate { it.id to (it.notes ?: "") }
                    uiState = PerformanceDetailUiState.Success(performance)
                }
        }
        viewModelScope.launch {
            snapshotFlow { draftNotes }
                .drop(1)
                .debounce(1000L)
                .collect { autoSaveNotes(it) }
        }
        loadPerformance()
    }

    fun loadPerformance() {
        viewModelScope.launch {
            uiState = PerformanceDetailUiState.Loading
            uiState = try {
                val performance = performancesRepository.getPerformance(performanceId)
                draftNotes = performance.setList.associate { it.id to (it.notes ?: "") }
                PerformanceDetailUiState.Success(performance)
            } catch (e: Exception) {
                PerformanceDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateDraftNote(entryId: String, notes: String) {
        draftNotes = draftNotes + (entryId to notes)
    }

    private fun autoSaveNotes(notes: Map<String, String>) {
        val performance = (uiState as? PerformanceDetailUiState.Success)?.performance ?: return
        viewModelScope.launch {
            try {
                val changedNotes = notes.filter { (entryId, note) ->
                    val original = performance.setList.find { it.id == entryId }?.notes ?: ""
                    note != original
                }
                if (changedNotes.isEmpty()) return@launch
                changedNotes.forEach { (entryId, note) ->
                    setListEntriesRepository.updateSetListEntry(
                        entryId,
                        SetListEntryRequest(notes = note.ifBlank { null })
                    )
                }
                // Advance the baseline so subsequent debounce fires only save new changes
                val updatedSetList = performance.setList.map { entry ->
                    entry.copy(notes = notes[entry.id]?.ifBlank { null } ?: entry.notes)
                }
                uiState = PerformanceDetailUiState.Success(performance.copy(setList = updatedSetList))
            } catch (_: Exception) {
                // TODO: surface error to user
            }
        }
    }
}

sealed interface PerformanceDetailUiState {
    data object Loading : PerformanceDetailUiState
    data class Success(val performance: Performance) : PerformanceDetailUiState
    data class Error(val message: String) : PerformanceDetailUiState
}
