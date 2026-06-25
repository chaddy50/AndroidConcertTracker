package com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.api.ApiErrorType
import com.chaddy50.concerttracker.data.api.ApiResult
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
            when (val result = repository.getUpcomingPerformances()) {
                is ApiResult.Success -> uiState = UpcomingTabUiState.Success(result.data)
                is ApiResult.Error -> uiState = UpcomingTabUiState.Error(result.errorType)
            }
        }
    }
}

sealed interface UpcomingTabUiState {
    data object Loading : UpcomingTabUiState
    data class Success(val performances: List<Performance>) : UpcomingTabUiState
    data class Error(val errorType: ApiErrorType.Type) : UpcomingTabUiState
}
