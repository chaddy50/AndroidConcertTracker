package com.chaddy50.concerttracker.ui.performanceEdit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.PerformanceRequest
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.navigation.PerformanceEdit
import com.chaddy50.concerttracker.util.epochMillisToIso
import com.chaddy50.concerttracker.util.isoToEpochMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerformanceEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val performancesRepository: PerformancesRepository
) : ViewModel() {

    private val performanceId: String? = savedStateHandle.toRoute<PerformanceEdit>().id

    val isCreateMode: Boolean = performanceId == null

    var uiState: PerformanceEditUiState by mutableStateOf(
        if (isCreateMode) PerformanceEditUiState.Ready else PerformanceEditUiState.Loading
    )
        private set

    var draftDate: Long? by mutableStateOf(null)
        private set

    var draftStatus: PerformanceStatus? by mutableStateOf(
        if (isCreateMode) PerformanceStatus.UPCOMING else null
    )
        private set

    var draftVenueId: String? by mutableStateOf(null)
        private set

    var draftVenueName: String? by mutableStateOf(null)
        private set

    var isSaving: Boolean by mutableStateOf(false)
        private set

    val canSave: Boolean
        get() = draftDate != null && draftVenueId != null

    private var loadedPerformance: Performance? = null

    init {
        if (!isCreateMode) loadPerformance()
    }

    fun loadPerformance() {
        viewModelScope.launch {
            uiState = PerformanceEditUiState.Loading
            try {
                val performance = performancesRepository.getPerformance(performanceId!!)
                loadedPerformance = performance
                draftDate = isoToEpochMillis(performance.date)
                draftStatus = performance.status
                draftVenueId = performance.venue.id
                draftVenueName = performance.venue.name
                uiState = PerformanceEditUiState.Ready
            } catch (e: Exception) {
                uiState = PerformanceEditUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateDraftDate(millis: Long) {
        draftDate = millis
    }

    fun updateDraftStatus(status: PerformanceStatus) {
        draftStatus = status
    }

    fun updateDraftVenue(venueId: String, venueName: String) {
        draftVenueId = venueId
        draftVenueName = venueName
    }

    fun savePerformance(onSaved: () -> Unit) {
        val date = draftDate?.let { epochMillisToIso(it) } ?: return
        val status = draftStatus ?: return
        val venueId = draftVenueId ?: return

        viewModelScope.launch {
            isSaving = true
            try {
                if (isCreateMode) {
                    performancesRepository.createPerformance(
                        PerformanceRequest(
                            date = date,
                            venueId = venueId,
                            performerIds = emptyList(),
                            conductorId = null,
                            status = status
                        )
                    )
                } else {
                    val performance = loadedPerformance!!
                    performancesRepository.updatePerformance(
                        performanceId!!,
                        PerformanceRequest(
                            date = date,
                            venueId = venueId,
                            performerIds = performance.performers.map { it.id },
                            conductorId = performance.conductor?.id,
                            status = status
                        )
                    )
                }
                onSaved()
            } catch (_: Exception) {
                // TODO: surface error to user
            } finally {
                isSaving = false
            }
        }
    }
}

sealed interface PerformanceEditUiState {
    data object Loading : PerformanceEditUiState
    data object Ready : PerformanceEditUiState
    data class Error(val message: String) : PerformanceEditUiState
}
