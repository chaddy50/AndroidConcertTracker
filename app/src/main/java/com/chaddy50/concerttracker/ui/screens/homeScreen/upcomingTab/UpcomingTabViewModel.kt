package com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class UpcomingTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(false)

    val uiState: StateFlow<UpcomingTabUiState> = combine(
        repository.observeUpcomingPerformances(),
        isLoading
    ) { performances, loading ->
        when {
            performances.isNotEmpty() -> UpcomingTabUiState.Content(buildUpcomingListItems(performances))
            loading -> UpcomingTabUiState.Loading
            else -> UpcomingTabUiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UpcomingTabUiState.Loading
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

sealed interface UpcomingTabUiState {
    data object Loading : UpcomingTabUiState
    data object Empty : UpcomingTabUiState
    data class Content(val items: List<UpcomingListItem>) : UpcomingTabUiState
}
