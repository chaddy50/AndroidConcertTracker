package com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(false)
    private val loadError = MutableStateFlow<ApiErrorType.Type?>(null)

    val uiState: StateFlow<CurrentTabUiState> = combine(
        repository.observeNextUpcomingPerformance(),
        repository.observeRecentlyAttendedPerformances(),
        isLoading,
        loadError
    ) { nextUpcoming, recentlyAttended, loading, error ->
        when {
            loading -> CurrentTabUiState.Loading
            error != null -> CurrentTabUiState.Error(error)
            nextUpcoming == null && recentlyAttended.isEmpty() -> CurrentTabUiState.Empty
            else -> CurrentTabUiState.Content(nextUpcoming, recentlyAttended)
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
            loadError.value = null
            val result = repository.loadPerformances()
            if (result is ApiResult.Error) {
                loadError.value = result.errorType
            }
            isLoading.value = false
        }
    }
}

sealed interface CurrentTabUiState {
    data object Loading : CurrentTabUiState
    data class Error(val errorType: ApiErrorType.Type) : CurrentTabUiState
    data object Empty : CurrentTabUiState
    data class Content(
        val nextUpcoming: Performance?,
        val recentlyAttended: List<Performance>
    ) : CurrentTabUiState
}
