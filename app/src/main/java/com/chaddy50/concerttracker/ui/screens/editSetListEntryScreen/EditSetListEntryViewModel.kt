package com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen

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
import com.chaddy50.concerttracker.data.external.api.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.external.api.SetListEntryUpdateRequest
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.navigation.routes.SetListEntryEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@kotlinx.serialization.Serializable
data class DraftFeaturedPerformer(
    val performerId: String,
    val name: String,
    val role: String = ""
)

@HiltViewModel
class EditSetListEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val performancesRepository: PerformancesRepository,
    private val setListEntriesRepository: SetListEntriesRepository
) : ViewModel() {

    private val route: SetListEntryEdit = savedStateHandle.toRoute()
    private val performanceId: String? = route.performanceId
    private val entryId: String? = route.entryId
    private val pendingLocalId: String? = route.pendingLocalId

    val isCreateMode: Boolean = entryId == null

    var uiState: SetListEntryEditUiState by mutableStateOf(SetListEntryEditUiState.Loading)
        private set

    var draftWorkId: String? by mutableStateOf(null)
        private set

    var draftWorkTitle: String? by mutableStateOf(null)
        private set

    var draftComposerName: String by mutableStateOf("")
        private set

    private var entryOrder: Int = 1

    val draftFeaturedPerformers = mutableStateListOf<DraftFeaturedPerformer>()

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var isDeleting: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    val canSave: Boolean
        get() = draftWorkId != null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = SetListEntryEditUiState.Loading
            if (pendingLocalId != null) {
                draftWorkId = route.pendingWorkId
                draftWorkTitle = route.pendingWorkTitle
                draftComposerName = route.pendingComposerName ?: ""
                entryOrder = route.pendingOrder ?: 1
                route.pendingFeaturedPerformersJson?.let { json ->
                    val performers: List<DraftFeaturedPerformer> =
                        kotlinx.serialization.json.Json.decodeFromString(json)
                    draftFeaturedPerformers.clear()
                    draftFeaturedPerformers.addAll(performers)
                }
                uiState = SetListEntryEditUiState.Ready
            } else if (performanceId != null) {
                val performance = performancesRepository.getPerformance(performanceId)
                if (performance == null) {
                    uiState = SetListEntryEditUiState.NotFound
                    return@launch
                }
                if (isCreateMode) {
                    entryOrder = performance.setList.size + 1
                } else {
                    val entry = performance.setList.find { it.id == entryId }
                    if (entry == null) {
                        uiState = SetListEntryEditUiState.NotFound
                        return@launch
                    }
                    draftWorkId = entry.work.id
                    draftWorkTitle = entry.work.title
                    draftComposerName = entry.work.composers.joinToString(", ") { it.sortName ?: it.name }
                    entryOrder = entry.order
                    draftFeaturedPerformers.clear()
                    draftFeaturedPerformers.addAll(
                        entry.featuredPerformers.map { featuredPerformer ->
                            DraftFeaturedPerformer(
                                performerId = featuredPerformer.performer.id,
                                name = featuredPerformer.performer.name,
                                role = featuredPerformer.role ?: ""
                            )
                        }
                    )
                }
                uiState = SetListEntryEditUiState.Ready
            } else {
                uiState = SetListEntryEditUiState.Ready
            }
        }
    }

    fun selectWork(workId: String, title: String, composerName: String) {
        draftWorkId = workId
        draftWorkTitle = title
        draftComposerName = composerName
    }

    fun addDraftFeaturedPerformer(
        performerId: String,
        performerName: String,
        performerTypeName: String?,
        specialty: String?
    ) {
        if (draftFeaturedPerformers.none { it.performerId == performerId }) {
            draftFeaturedPerformers.add(DraftFeaturedPerformer(performerId, performerName))
        }
    }

    fun updateDraftFeaturedPerformerRole(performerId: String, role: String) {
        val index = draftFeaturedPerformers.indexOfFirst { it.performerId == performerId }
        if (index != -1) {
            draftFeaturedPerformers[index] = draftFeaturedPerformers[index].copy(role = role)
        }
    }

    fun removeDraftFeaturedPerformer(performerId: String) {
        draftFeaturedPerformers.removeAll { it.performerId == performerId }
    }

    fun moveDraftFeaturedPerformer(from: Int, to: Int) {
        if (from == to || from !in draftFeaturedPerformers.indices || to !in draftFeaturedPerformers.indices) return
        val item = draftFeaturedPerformers.removeAt(from)
        draftFeaturedPerformers.add(to, item)
    }

    fun saveSetListEntry(
        onSaved: () -> Unit,
        onSavedAsPending: ((PendingEntryResult) -> Unit)? = null
    ) {
        val workId = draftWorkId ?: return
        val workTitle = draftWorkTitle ?: return
        val order = entryOrder

        if (performanceId == null) {
            onSavedAsPending?.invoke(
                PendingEntryResult(
                    pendingLocalId = pendingLocalId,
                    workId = workId,
                    workTitle = workTitle,
                    composerName = draftComposerName,
                    order = order,
                    featuredPerformers = draftFeaturedPerformers.toList()
                )
            )
            return
        }

        val featuredPerformerRequests = draftFeaturedPerformers.mapIndexed { index, draftPerformer ->
            FeaturedPerformerRequest(
                performerId = draftPerformer.performerId,
                role = draftPerformer.role,
                order = index
            )
        }

        viewModelScope.launch {
            isSaving = true
            saveError = null
            val result = if (isCreateMode) {
                setListEntriesRepository.createSetListEntry(
                    SetListEntryCreateRequest(
                        performanceId = performanceId,
                        workId = workId,
                        order = order,
                        featuredPerformers = featuredPerformerRequests
                    )
                )
            } else {
                setListEntriesRepository.updateSetListEntryFull(
                    entryId!!,
                    SetListEntryUpdateRequest(
                        workId = workId,
                        order = order,
                        featuredPerformers = featuredPerformerRequests
                    )
                )
            }
            when (result) {
                is ApiResult.Success -> onSaved()
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
            isSaving = false
        }
    }

    data class PendingEntryResult(
        val pendingLocalId: String?,
        val workId: String,
        val workTitle: String,
        val composerName: String,
        val order: Int,
        val featuredPerformers: List<DraftFeaturedPerformer>
    )

    fun deleteSetListEntry(onDeleted: () -> Unit) {
        val id = entryId ?: return
        viewModelScope.launch {
            isDeleting = true
            when (val result = setListEntriesRepository.deleteSetListEntry(id)) {
                is ApiResult.Success -> onDeleted()
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
            isDeleting = false
        }
    }
}

sealed interface SetListEntryEditUiState {
    data object Loading : SetListEntryEditUiState
    data object Ready : SetListEntryEditUiState
    data object NotFound : SetListEntryEditUiState
}
