package com.chaddy50.concerttracker.ui.performanceEdit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.PerformanceRequest
import com.chaddy50.concerttracker.data.entity.PerformerRequest
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.navigation.PerformanceEdit
import com.chaddy50.concerttracker.util.epochMillisToIso
import com.chaddy50.concerttracker.util.isoToEpochMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerformanceEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val performancesRepository: PerformancesRepository,
    private val performersRepository: PerformersRepository
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

    var draftConductorId: String? by mutableStateOf(null)
        private set

    var draftConductorName: String? by mutableStateOf(null)
        private set

    val draftPerformers = mutableStateListOf<Pair<String, String>>()

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
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
                draftConductorId = performance.conductor?.id
                draftConductorName = performance.conductor?.name
                draftPerformers.clear()
                draftPerformers.addAll(performance.performers.map { it.id to it.name })
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

    fun addDraftPerformer(performerId: String, performerName: String, performerTypeName: String?) {
        if (draftPerformers.any { it.first == performerId }) return
        val type = performerTypeName?.let { runCatching { PerformerType.valueOf(it) }.getOrNull() }
            ?: PerformerType.OTHER
        viewModelScope.launch {
            try {
                val performer = performersRepository.createPerformer(
                    PerformerRequest(performerId, performerName, type)
                )
                draftPerformers.add(performer.id to performer.name)
            } catch (e: Exception) {
                saveError = "Failed to add performer: ${e.message}"
            }
        }
    }

    fun updateDraftConductor(conductorId: String, conductorName: String) {
        viewModelScope.launch {
            try {
                val conductor = performersRepository.createPerformer(
                    PerformerRequest(conductorId, conductorName, PerformerType.CONDUCTOR)
                )
                draftConductorId = conductor.id
                draftConductorName = conductor.name
            } catch (e: Exception) {
                saveError = "Failed to add conductor: ${e.message}"
            }
        }
    }

    fun removeDraftPerformer(performerId: String) {
        draftPerformers.removeAll { it.first == performerId }
    }

    fun savePerformance(onSaved: () -> Unit) {
        val date = draftDate?.let { epochMillisToIso(it) } ?: return
        val status = draftStatus ?: return
        val venueId = draftVenueId ?: return

        viewModelScope.launch {
            isSaving = true
            try {
                val performerIds = draftPerformers.map { it.first }
                if (isCreateMode) {
                    performancesRepository.createPerformance(
                        PerformanceRequest(
                            date = date,
                            venueId = venueId,
                            performerIds = performerIds,
                            conductorId = draftConductorId,
                            status = status
                        )
                    )
                } else {
                    performancesRepository.updatePerformance(
                        performanceId!!,
                        PerformanceRequest(
                            date = date,
                            venueId = venueId,
                            performerIds = performerIds,
                            conductorId = draftConductorId,
                            status = status
                        )
                    )
                }
                onSaved()
            } catch (e: Exception) {
                saveError = "Failed to save: ${e.message}"
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
