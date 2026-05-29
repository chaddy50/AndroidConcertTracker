package com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab

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
class UpcomingTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    var uiState: UpcomingTabUiState by mutableStateOf(UpcomingTabUiState.Loading)
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = UpcomingTabUiState.Loading
            try {
                val performances = repository.getUpcomingPerformances()
                uiState = UpcomingTabUiState.Success(performances)
            } catch (e: Exception) {
                uiState = UpcomingTabUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface UpcomingTabUiState {
    data object Loading : UpcomingTabUiState
    data class Success(val performances: List<Performance>) : UpcomingTabUiState
    data class Error(val message: String) : UpcomingTabUiState
}
