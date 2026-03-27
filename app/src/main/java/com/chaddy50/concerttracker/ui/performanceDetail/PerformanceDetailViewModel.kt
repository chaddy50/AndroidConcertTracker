package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.navigation.PerformanceDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerformanceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PerformancesRepository
) : ViewModel() {

    private val performanceId: String = savedStateHandle.toRoute<PerformanceDetail>().id

    var uiState: PerformanceDetailUiState by mutableStateOf(PerformanceDetailUiState.Loading)
        private set

    init {
        loadPerformance()
    }

    fun loadPerformance() {
        viewModelScope.launch {
            uiState = PerformanceDetailUiState.Loading
            uiState = try {
                PerformanceDetailUiState.Success(repository.getPerformance(performanceId))
            } catch (e: Exception) {
                PerformanceDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface PerformanceDetailUiState {
    data object Loading : PerformanceDetailUiState
    data class Success(val performance: Performance) : PerformanceDetailUiState
    data class Error(val message: String) : PerformanceDetailUiState
}