package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

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
class PastTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {

    var uiState: PastTabUiState by mutableStateOf(PastTabUiState.Loading)
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = PastTabUiState.Loading
            when (val result = repository.getPastPerformances()) {
                is ApiResult.Success -> uiState = PastTabUiState.Success(result.data)
                is ApiResult.Error -> uiState = PastTabUiState.Error(result.errorType)
            }
        }
    }
}

sealed interface PastTabUiState {
    data object Loading : PastTabUiState
    data class Success(val performances: List<Performance>) : PastTabUiState
    data class Error(val errorType: ApiErrorType.Type) : PastTabUiState
}
