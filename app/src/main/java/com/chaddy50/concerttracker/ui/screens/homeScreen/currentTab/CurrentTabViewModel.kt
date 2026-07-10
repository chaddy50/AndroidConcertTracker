package com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab

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
class CurrentTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(false)

    val uiState: StateFlow<CurrentTabUiState> = combine(
        repository.observeNextUpcomingPerformance(),
        repository.observeRecentlyAttendedPerformances(),
        isLoading
    ) { nextUpcoming, recentlyAttended, loading ->
        // Offline-first: Room is the source of truth, so the screen reflects the cache. A failed
        // background refresh is never surfaced here — no content simply means Empty.
        when {
            nextUpcoming != null || recentlyAttended.isNotEmpty() ->
                CurrentTabUiState.Content(nextUpcoming, recentlyAttended)
            loading -> CurrentTabUiState.Loading
            else -> CurrentTabUiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CurrentTabUiState.Loading
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

sealed interface CurrentTabUiState {
    data object Loading : CurrentTabUiState
    data object Empty : CurrentTabUiState
    data class Content(
        val nextUpcoming: Performance?,
        val recentlyAttended: List<Performance>
    ) : CurrentTabUiState
}
