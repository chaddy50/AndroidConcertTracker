package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PastTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(false)
    private val loadError = MutableStateFlow<ApiErrorType.Type?>(null)

    val uiState: StateFlow<PastTabUiState> = combine(
        repository.observePastPerformances(),
        isLoading,
        loadError
    ) { performances, loading, error ->
        when {
            loading -> PastTabUiState.Loading
            error != null -> PastTabUiState.Error(error)
            performances.isEmpty() -> PastTabUiState.Empty
            else -> PastTabUiState.Content(performances)
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
            loadError.value = null
            val result = repository.loadPerformances()
            if (result is ApiResult.Error) {
                loadError.value = result.errorType
            }
            isLoading.value = false
        }
    }
}

sealed interface PastTabUiState {
    data object Loading : PastTabUiState
    data class Error(val errorType: ApiErrorType.Type) : PastTabUiState
    data object Empty : PastTabUiState
    data class Content(val performances: List<Performance>) : PastTabUiState
}
