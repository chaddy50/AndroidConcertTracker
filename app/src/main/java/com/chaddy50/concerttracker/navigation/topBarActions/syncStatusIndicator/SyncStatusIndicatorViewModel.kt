package com.chaddy50.concerttracker.navigation.topBarActions.syncStatusIndicator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.domain.SyncJob
import com.chaddy50.concerttracker.data.repository.SyncOperationsRepository
import com.chaddy50.concerttracker.data.sync.SyncScheduler
import com.chaddy50.concerttracker.util.SyncJobDescriber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SyncStatus(val jobs: List<SyncJob>) {
    val queuedCount: Int get() = jobs.size
    val hasWork: Boolean get() = jobs.isNotEmpty()
    val hasFailure: Boolean get() = jobs.any { it.failed }
}

@HiltViewModel
class SyncStatusIndicatorViewModel @Inject constructor(
    private val syncOperationsRepository: SyncOperationsRepository,
    private val syncJobDescriber: SyncJobDescriber,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    val uiState: StateFlow<SyncStatus> = syncOperationsRepository.observeJobs()
        .map { jobs -> SyncStatus(jobs.map { syncJobDescriber.describe(it) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus(emptyList()))

    fun retry() = syncScheduler.requestImmediateSync()
}
