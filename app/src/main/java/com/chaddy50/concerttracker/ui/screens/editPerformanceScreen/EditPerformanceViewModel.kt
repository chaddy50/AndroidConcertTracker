package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.external.api.ApiErrorType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.FeaturedPerformerRequest
import com.chaddy50.concerttracker.data.external.api.PerformanceRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryInlineRequest
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.navigation.routes.PerformanceEdit
import com.chaddy50.concerttracker.util.epochMillisToIso
import com.chaddy50.concerttracker.util.isoToEpochMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPerformanceViewModel @Inject constructor(
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
        if (!isCreateMode) {
            loadPerformance()
            observeSetList()
        }
    }

    /**
     * The set list isn't part of the editable draft — its entries are persisted immediately by the
     * entry sub-screen — so it's observed straight from Room and stays in sync after any add/edit/
     * delete write-through without a manual refresh.
     */
    private fun observeSetList() {
        val id = performanceId ?: return
        viewModelScope.launch {
            performancesRepository.observePerformance(id).collect { performance ->
                currentSetList.clear()
                currentSetList.addAll(performance?.setList?.sortedBy { it.order } ?: emptyList())
            }
        }
    }

    fun loadPerformance() {
        viewModelScope.launch {
            uiState = PerformanceEditUiState.Loading
            when (val result = performancesRepository.getPerformance(performanceId!!)) {
                is ApiResult.Success -> {
                    val performance = result.data
                    loadedPerformance = performance
                    draftDate = isoToEpochMillis(performance.date)
                    draftStatus = performance.status
                    draftVenueId = performance.venue.id
                    draftVenueName = performance.venue.name
                    draftPerformers.clear()
                    draftPerformers.addAll(performance.performers)
                    uiState = PerformanceEditUiState.Ready
                }
                is ApiResult.Error -> uiState = PerformanceEditUiState.Error(result.errorType)
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
        if (draftPerformers.any { it.id == performerId }) return
        val type = performerTypeName?.let { runCatching { PerformerType.valueOf(it) }.getOrNull() }
            ?: PerformerType.OTHER
        draftPerformers.add(Performer(id = performerId, name = performerName, type = type, specialty = specialty))
    }

    fun removeDraftPerformer(performerId: String) {
        draftPerformers.removeAll { it.id == performerId }
    }

    fun addPendingSetListEntry(
        workId: String,
        workTitle: String,
        composerName: String,
        order: Int,
        featuredPerformers: List<PendingFeaturedPerformer>
    ) {
        pendingSetListEntries.add(
            PendingSetListEntry(java.util.UUID.randomUUID().toString(), workId, workTitle, composerName, order, featuredPerformers)
        )
    }

    fun replacePendingSetListEntry(
        localId: String,
        workId: String,
        workTitle: String,
        composerName: String,
        order: Int,
        featuredPerformers: List<PendingFeaturedPerformer>
    ) {
        val index = pendingSetListEntries.indexOfFirst { it.localId == localId }
        if (index != -1) {
            pendingSetListEntries[index] = PendingSetListEntry(localId, workId, workTitle, composerName, order, featuredPerformers)
        }
    }

    fun savePerformance(onSaved: () -> Unit) {
        val date = draftDate?.let { epochMillisToIso(it) } ?: return
        val status = draftStatus ?: return
        val venueId = draftVenueId ?: return
        val performerIds = draftPerformers.map { it.id }

        viewModelScope.launch {
            isSaving = true
            val result = if (isCreateMode) {
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
                    PerformanceRequest(date = date, venueId = venueId, performerIds = performerIds, status = status)
                )
            }
            when (result) {
                is ApiResult.Success -> onSaved()
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
            isSaving = false
        }
    }

    fun deletePerformance(onDeleted: () -> Unit) {
        val id = performanceId ?: return
        viewModelScope.launch {
            when (val result = performancesRepository.deletePerformance(id)) {
                is ApiResult.Success -> onDeleted()
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
        }
    }
}

data class PendingSetListEntry(
    val localId: String,
    val workId: String,
    val workTitle: String,
    val composerName: String,
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
    data class Error(val errorType: ApiErrorType.Type) : PerformanceEditUiState
}
