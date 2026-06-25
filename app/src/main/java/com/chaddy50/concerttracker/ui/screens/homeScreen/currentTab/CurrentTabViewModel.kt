package com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab

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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    var uiState: CurrentTabUiState by mutableStateOf(CurrentTabUiState.Loading)
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = CurrentTabUiState.Loading
            val nextDeferred = async { repository.getNextUpcomingPerformance() }
            val recentDeferred = async { repository.getRecentlyAttendedPerformances() }
            val nextResult = nextDeferred.await()
            val recentResult = recentDeferred.await()
            uiState = when {
                nextResult is ApiResult.Error -> CurrentTabUiState.Error(nextResult.errorType)
                recentResult is ApiResult.Error -> CurrentTabUiState.Error(recentResult.errorType)
                else -> CurrentTabUiState.Success(
                    nextUpcoming = (nextResult as ApiResult.Success).data,
                    recentAttended = (recentResult as ApiResult.Success).data
                )
            }
        }
    }
}

sealed interface CurrentTabUiState {
    data object Loading : CurrentTabUiState
    data class Success(
        val nextUpcoming: Performance?,
        val recentAttended: List<Performance>
    ) : CurrentTabUiState
    data class Error(val errorType: ApiErrorType.Type) : CurrentTabUiState
}
