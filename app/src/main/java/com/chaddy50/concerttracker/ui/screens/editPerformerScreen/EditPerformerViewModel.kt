package com.chaddy50.concerttracker.ui.screens.editPerformerScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.repository.PerformersRepository
import com.chaddy50.concerttracker.navigation.routes.PerformerEdit
import com.chaddy50.concerttracker.navigation.routes.PerformerUpdatedResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPerformerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val performersRepository: PerformersRepository
) : ViewModel() {

    private val performerId: String = savedStateHandle.toRoute<PerformerEdit>().performerId

    var uiState: PerformerEditUiState by mutableStateOf(PerformerEditUiState.Loading)
        private set

    var draftName: String by mutableStateOf("")
        private set

    var draftType: PerformerType by mutableStateOf(PerformerType.OTHER)
        private set

    var draftSpecialty: String? by mutableStateOf(null)
        private set

    private var musicbrainzId: String? = null

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    var isNameEditable: Boolean by mutableStateOf(false)
        private set

    val canSave: Boolean
        get() = uiState is PerformerEditUiState.Ready && draftName.isNotBlank()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            uiState = PerformerEditUiState.Loading
            val performer = performersRepository.getPerformer(performerId)
            if (performer == null) {
                uiState = PerformerEditUiState.NotFound
                return@launch
            }
            draftName = performer.name
            draftType = performer.type
            draftSpecialty = performer.specialty
            musicbrainzId = performer.musicbrainzId
            uiState = PerformerEditUiState.Ready
        }
    }

    fun enableNameEditing() {
        isNameEditable = true
    }

    fun updateDraftName(text: String) {
        draftName = text
    }

    fun updateDraftType(type: PerformerType) {
        draftType = type
    }

    fun updateDraftSpecialty(text: String) {
        draftSpecialty = text.ifBlank { null }
    }

    fun savePerformer(onSaved: (PerformerUpdatedResult) -> Unit) {
        viewModelScope.launch {
            isSaving = true
            saveError = null
            when (val result = performersRepository.updatePerformer(
                performerId, draftName, draftType, draftSpecialty, musicbrainzId
            )) {
                is ApiResult.Success -> onSaved(
                    PerformerUpdatedResult(performerId, draftName, draftType.name, draftSpecialty)
                )
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
            isSaving = false
        }
    }
}

sealed interface PerformerEditUiState {
    data object Loading : PerformerEditUiState
    data object Ready : PerformerEditUiState
    data object NotFound : PerformerEditUiState
}
