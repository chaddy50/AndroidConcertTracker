package com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
            try {
                coroutineScope {
                    val nextUpcomingDeferred = async { repository.getNextUpcomingPerformance() }
                    val recentAttendedDeferred = async { repository.getRecentlyAttendedPerformances() }
                    uiState = CurrentTabUiState.Success(
                        nextUpcoming = nextUpcomingDeferred.await(),
                        recentAttended = recentAttendedDeferred.await()
                    )
                }
            } catch (e: Exception) {
                uiState = CurrentTabUiState.Error(e.message ?: "Unknown error")
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
    data class Error(val message: String) : CurrentTabUiState
}
