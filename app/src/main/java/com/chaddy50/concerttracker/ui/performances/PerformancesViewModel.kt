package com.chaddy50.concerttracker.ui.performances

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerformancesViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    var uiState: PerformancesUiState by mutableStateOf(PerformancesUiState.Loading)
        private set

    init {
        loadPerformances()
    }

    fun loadPerformances() {
        viewModelScope.launch {
            uiState = PerformancesUiState.Loading
            uiState = try {
                PerformancesUiState.Success(repository.getPerformances())
            } catch (e: Exception) {
                PerformancesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface PerformancesUiState {
    data object Loading : PerformancesUiState
    data class Success(val performances: List<Performance>) : PerformancesUiState
    data class Error(val message: String) : PerformancesUiState
}