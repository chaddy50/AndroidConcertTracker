package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
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
    private val setListEntriesRepository: SetListEntriesRepository
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

    var currentSetList: List<SetListEntry> by mutableStateOf(emptyList())
        private set

    var pendingSetListEntries: List<PendingSetListEntry> by mutableStateOf(emptyList())
        private set

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    val canSave: Boolean
        get() = draftDate != null && draftVenueId != null

    private var loadedPerformance: Performance? = null

    private var wasSetListReordered = false

    init {
        if (!isCreateMode) {
            loadPerformance()
            observeSetList()
        }
    }

    /**
     * Set list entries are added/edited/deleted immediately by the sub-screen (write-through), so the
     * list is observed straight from Room. Reordering, however, is a draft committed on save, so each
     * emission is reconciled against the current draft order: entries that still exist keep their
     * drafted position, deletions drop out, and newly added entries are appended (in server order).
     */
    private fun observeSetList() {
        val id = performanceId ?: return
        viewModelScope.launch {
            performancesRepository.observePerformance(id).collect { performance ->
                val roomEntries = performance?.setList ?: emptyList()
                val draftOrder = currentSetList.map { it.id }
                val byId = roomEntries.associateBy { it.id }
                val known = draftOrder.toSet()
                currentSetList = draftOrder.mapNotNull { byId[it] } +
                    roomEntries.filter { it.id !in known }.sortedBy { it.order }
            }
        }
    }

    fun loadPerformance() {
        viewModelScope.launch {
            uiState = PerformanceEditUiState.Loading
            val performance = performancesRepository.getPerformance(performanceId!!)
            if (performance != null) {
                loadedPerformance = performance
                draftDate = isoToEpochMillis(performance.date)
                draftStatus = performance.status
                draftVenueId = performance.venue.id
                draftVenueName = performance.venue.name
                draftPerformers.clear()
                draftPerformers.addAll(performance.performers)
                uiState = PerformanceEditUiState.Ready
            } else {
                uiState = PerformanceEditUiState.NotFound
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

    /** A newly added pending entry goes to the end; its order is its 1-based position. */
    fun addPendingSetListEntry(
        workId: String,
        workTitle: String,
        composerName: String,
        featuredPerformers: List<PendingFeaturedPerformer>
    ) {
        pendingSetListEntries = pendingSetListEntries + PendingSetListEntry(
            java.util.UUID.randomUUID().toString(),
            workId,
            workTitle,
            composerName,
            pendingSetListEntries.size + 1,
            featuredPerformers
        )
    }

    /** Editing a pending entry in place keeps its existing position/order. */
    fun replacePendingSetListEntry(
        localId: String,
        workId: String,
        workTitle: String,
        composerName: String,
        featuredPerformers: List<PendingFeaturedPerformer>
    ) {
        val index = pendingSetListEntries.indexOfFirst { it.localId == localId }
        if (index != -1) {
            val existingOrder = pendingSetListEntries[index].order
            pendingSetListEntries = pendingSetListEntries.toMutableList().apply {
                this[index] = PendingSetListEntry(localId, workId, workTitle, composerName, existingOrder, featuredPerformers)
            }
        }
    }

    fun moveSetListEntry(from: Int, to: Int) {
        if (from == to || from !in currentSetList.indices || to !in currentSetList.indices) return
        currentSetList = currentSetList.toMutableList().apply { add(to, removeAt(from)) }
        wasSetListReordered = true
    }

    fun movePendingSetListEntry(from: Int, to: Int) {
        if (from == to || from !in pendingSetListEntries.indices || to !in pendingSetListEntries.indices) return
        val reordered = pendingSetListEntries.toMutableList().apply { add(to, removeAt(from)) }
        pendingSetListEntries = reordered.mapIndexed { index, entry -> entry.copy(order = index + 1) }
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
                is ApiResult.Success -> {
                    if (!isCreateMode && wasSetListReordered) {
                        setListEntriesRepository.reorderSetListEntries(currentSetList.map { it.id })
                    }
                    onSaved()
                }
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
    data object NotFound : PerformanceEditUiState
}
