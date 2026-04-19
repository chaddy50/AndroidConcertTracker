package com.chaddy50.concerttracker.ui.setListEntryEdit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.entity.FeaturedPerformerRequest
import com.chaddy50.concerttracker.data.entity.PerformerRequest
import com.chaddy50.concerttracker.data.entity.SetListEntryCreateRequest
import com.chaddy50.concerttracker.data.entity.SetListEntryUpdateRequest
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.data.repository.SetListEntriesRepository
import com.chaddy50.concerttracker.data.repository.WorksRepository
import com.chaddy50.concerttracker.navigation.SetListEntryEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftFeaturedPerformer(
    val performerId: String,
    val name: String,
    val role: String = ""
)

@HiltViewModel
class SetListEntryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val performancesRepository: PerformancesRepository,
    private val performersRepository: PerformersRepository,
    private val worksRepository: WorksRepository,
    private val setListEntriesRepository: SetListEntriesRepository
) : ViewModel() {

    private val performanceId: String = savedStateHandle.toRoute<SetListEntryEdit>().performanceId
    private val entryId: String? = savedStateHandle.toRoute<SetListEntryEdit>().entryId

    val isCreateMode: Boolean = entryId == null

    var uiState: SetListEntryEditUiState by mutableStateOf(SetListEntryEditUiState.Loading)
        private set

    var draftWorkId: String? by mutableStateOf(null)
        private set

    var draftWorkTitle: String? by mutableStateOf(null)
        private set

    var draftOrder: String by mutableStateOf("1")
        private set

    var draftConductorId: String? by mutableStateOf(null)
        private set

    var draftConductorName: String? by mutableStateOf(null)
        private set

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
            try {
                val performance = performancesRepository.getPerformance(performanceId)
                if (isCreateMode) {
                    draftOrder = (performance.setList.size + 1).toString()
                } else {
                    val entry = performance.setList.find { it.id == entryId }
                        ?: error("Set list entry $entryId not found in performance $performanceId")
                    draftWorkId = entry.work.id
                    draftWorkTitle = entry.work.title
                    draftOrder = entry.order.toString()
                    draftConductorId = entry.conductor?.id
                    draftConductorName = entry.conductor?.name
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
            } catch (exception: Exception) {
                uiState = SetListEntryEditUiState.Error(exception.message ?: "Unknown error")
            }
        }
    }

    fun selectWork(openOpusWorkId: String, title: String, openOpusComposerId: String, composerName: String) {
        viewModelScope.launch {
            try {
                val work = worksRepository.createWorkFromOpenOpus(openOpusWorkId, title, openOpusComposerId, composerName)
                draftWorkId = work.id
                draftWorkTitle = work.title
            } catch (exception: Exception) {
                saveError = "Failed to add work: ${exception.message}"
            }
        }
    }

    fun updateDraftConductor(musicbrainzId: String, conductorName: String) {
        viewModelScope.launch {
            try {
                val conductor = performersRepository.createPerformer(
                    PerformerRequest(musicbrainzId, conductorName, PerformerType.CONDUCTOR)
                )
                draftConductorId = conductor.id
                draftConductorName = conductor.name
            } catch (exception: Exception) {
                saveError = "Failed to add conductor: ${exception.message}"
            }
        }
    }

    fun clearDraftConductor() {
        draftConductorId = null
        draftConductorName = null
    }

    fun addDraftFeaturedPerformer(
        musicbrainzId: String,
        performerName: String,
        performerTypeName: String?,
        specialty: String?
    ) {
        viewModelScope.launch {
            try {
                val type = performerTypeName
                    ?.let { runCatching { PerformerType.valueOf(it) }.getOrNull() }
                    ?: PerformerType.OTHER
                val performer = performersRepository.createPerformer(
                    PerformerRequest(musicbrainzId, performerName, type, specialty)
                )
                if (draftFeaturedPerformers.none { it.performerId == performer.id }) {
                    draftFeaturedPerformers.add(DraftFeaturedPerformer(performer.id, performer.name))
                }
            } catch (exception: Exception) {
                saveError = "Failed to add performer: ${exception.message}"
            }
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

    fun updateDraftOrder(value: String) {
        val parsed = value.toIntOrNull()
        if (value.isEmpty() || (parsed != null && parsed > 0)) {
            draftOrder = value
        }
    }

    fun saveSetListEntry(onSaved: () -> Unit) {
        val workId = draftWorkId ?: return
        val order = draftOrder.toIntOrNull()?.takeIf { it > 0 } ?: return
        val featuredPerformerRequests = draftFeaturedPerformers.map { draftPerformer ->
            FeaturedPerformerRequest(
                performerId = draftPerformer.performerId,
                role = draftPerformer.role.ifBlank { null }
            )
        }

        viewModelScope.launch {
            isSaving = true
            saveError = null
            try {
                if (isCreateMode) {
                    setListEntriesRepository.createSetListEntry(
                        SetListEntryCreateRequest(
                            performanceId = performanceId,
                            workId = workId,
                            order = order,
                            featuredPerformers = featuredPerformerRequests,
                            conductorId = draftConductorId
                        )
                    )
                } else {
                    setListEntriesRepository.updateSetListEntryFull(
                        entryId!!,
                        SetListEntryUpdateRequest(
                            workId = workId,
                            order = order,
                            featuredPerformers = featuredPerformerRequests,
                            conductorId = draftConductorId
                        )
                    )
                }
                onSaved()
            } catch (exception: Exception) {
                saveError = "Failed to save: ${exception.message}"
            } finally {
                isSaving = false
            }
        }
    }

    fun deleteSetListEntry(onDeleted: () -> Unit) {
        val id = entryId ?: return
        viewModelScope.launch {
            isDeleting = true
            try {
                setListEntriesRepository.deleteSetListEntry(id)
                onDeleted()
            } catch (exception: Exception) {
                saveError = "Failed to delete: ${exception.message}"
            } finally {
                isDeleting = false
            }
        }
    }
}

sealed interface SetListEntryEditUiState {
    data object Loading : SetListEntryEditUiState
    data object Ready : SetListEntryEditUiState
    data class Error(val message: String) : SetListEntryEditUiState
}
