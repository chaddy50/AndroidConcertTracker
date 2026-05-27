package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.api.FeaturedPerformerRequest
import com.chaddy50.concerttracker.data.api.PerformanceRequest
import com.chaddy50.concerttracker.data.api.PerformerRequest
import com.chaddy50.concerttracker.data.api.SetListEntryInlineRequest
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.navigation.routes.PerformanceEdit
import com.chaddy50.concerttracker.util.epochMillisToIso
import com.chaddy50.concerttracker.util.isoToEpochMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPerformanceViewModel @Inject constructor(
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

    val draftPerformers = mutableStateListOf<Performer>()

    val currentSetList = mutableStateListOf<SetListEntry>()

    val pendingSetListEntries = mutableStateListOf<PendingSetListEntry>()

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
                draftPerformers.clear()
                draftPerformers.addAll(performance.performers)
                currentSetList.clear()
                currentSetList.addAll(performance.setList.sortedBy { it.order })
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

    fun addDraftPerformer(performerId: String, performerName: String, performerTypeName: String?, specialty: String?) {
        if (draftPerformers.any { it.musicbrainzId == performerId }) return
        val type = performerTypeName?.let { runCatching { PerformerType.valueOf(it) }.getOrNull() }
            ?: PerformerType.OTHER
        viewModelScope.launch {
            try {
                val performer = performersRepository.createPerformer(
                    PerformerRequest(performerName, type, specialty, performerId)
                )
                draftPerformers.add(performer)
            } catch (e: Exception) {
                saveError = "Failed to add performer: ${e.message}"
            }
        }
    }

    fun removeDraftPerformer(performerId: String) {
        draftPerformers.removeAll { it.id == performerId }
    }

    fun addPendingSetListEntry(
        workId: String,
        workTitle: String,
        order: Int,
        featuredPerformers: List<PendingFeaturedPerformer>
    ) {
        pendingSetListEntries.add(
            PendingSetListEntry(java.util.UUID.randomUUID().toString(), workId, workTitle, order, featuredPerformers)
        )
    }

    fun replacePendingSetListEntry(
        localId: String,
        workId: String,
        workTitle: String,
        order: Int,
        featuredPerformers: List<PendingFeaturedPerformer>
    ) {
        val index = pendingSetListEntries.indexOfFirst { it.localId == localId }
        if (index != -1) {
            pendingSetListEntries[index] = PendingSetListEntry(localId, workId, workTitle, order, featuredPerformers)
        }
    }

    fun refreshSetList() {
        val id = performanceId ?: return
        viewModelScope.launch {
            try {
                val performance = performancesRepository.getPerformance(id)
                currentSetList.clear()
                currentSetList.addAll(performance.setList.sortedBy { it.order })
            } catch (exception: Exception) {
                // Silently ignore — the set list display may be stale but draft fields are unaffected
            }
        }
    }

    fun savePerformance(onSaved: () -> Unit) {
        val date = draftDate?.let { epochMillisToIso(it) } ?: return
        val status = draftStatus ?: return
        val venueId = draftVenueId ?: return

        viewModelScope.launch {
            isSaving = true
            try {
                val performerIds = draftPerformers.map { it.id }
                if (isCreateMode) {
                    performancesRepository.createPerformance(
                        PerformanceRequest(
                            date = date,
                            venueId = venueId,
                            performerIds = performerIds,
                            status = status,
                            setList = pendingSetListEntries.map { entry ->
                                SetListEntryInlineRequest(
                                    workId = entry.workId,
                                    order = entry.order,
                                    featuredPerformers = entry.featuredPerformers.map { p ->
                                        FeaturedPerformerRequest(p.performerId, p.role.ifBlank { null })
                                    }
                                )
                            }
                        )
                    )
                } else {
                    performancesRepository.updatePerformance(
                        performanceId!!,
                        PerformanceRequest(
                            date = date,
                            venueId = venueId,
                            performerIds = performerIds,
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

    fun deletePerformance(onDeleted: () -> Unit) {
        val id = performanceId ?: return
        viewModelScope.launch {
            try {
                performancesRepository.deletePerformance(id)
                onDeleted()
            } catch (e: Exception) {
                saveError = "Failed to delete: ${e.message}"
            }
        }
    }
}

data class PendingSetListEntry(
    val localId: String,
    val workId: String,
    val workTitle: String,
    val order: Int,
    val featuredPerformers: List<PendingFeaturedPerformer>
)

data class PendingFeaturedPerformer(
    val performerId: String,
    val name: String,
    val role: String
)

sealed interface PerformanceEditUiState {
    data object Loading : PerformanceEditUiState
    data object Ready : PerformanceEditUiState
    data class Error(val message: String) : PerformanceEditUiState
}
