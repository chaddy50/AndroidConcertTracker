package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PastTabViewModel @Inject constructor(
    private val repository: PerformancesRepository
) : ViewModel() {
    val pagedItems: Flow<PagingData<PastListItem>> =
        repository.observePastPerformancesPaged()
            .map { pagingData ->
                pagingData
                    .map<_, PastListItem> { PastListItem.Entry(it) }
                    .insertSeparators { before, after ->
                        pastListSeparator(before as? PastListItem.Entry, after as? PastListItem.Entry)
                    }
            }
            .cachedIn(viewModelScope)

    init {
        loadPerformances()
    }

    fun loadPerformances() {
        viewModelScope.launch {
            repository.loadPerformances()
        }
    }
}
