package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PastTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(false)

    val uiState: StateFlow<PastTabUiState> = combine(
        repository.observePastPerformances(),
        isLoading
    ) { performances, loading ->
        // Offline-first: Room is the source of truth, so the screen reflects the cache. A failed
        // background refresh is never surfaced here — no content simply means Empty.
        when {
            performances.isNotEmpty() -> PastTabUiState.Content(performances)
            loading -> PastTabUiState.Loading
            else -> PastTabUiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PastTabUiState.Loading
    )

    init {
        loadPerformances()
    }

    fun loadPerformances() {
        viewModelScope.launch {
            isLoading.value = true
            repository.loadPerformances()
            isLoading.value = false
        }
    }
}

sealed interface PastTabUiState {
    data object Loading : PastTabUiState
    data object Empty : PastTabUiState
    data class Content(val performances: List<Performance>) : PastTabUiState
}
