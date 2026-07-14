package com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.ServerUrlValidationError
import com.chaddy50.concerttracker.data.repository.ServerValidationRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import com.chaddy50.concerttracker.data.repository.isValidServerUrlFormat
import com.chaddy50.concerttracker.data.repository.toServerUrlValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerUrlPromptViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val serverValidationRepository: ServerValidationRepository,
    private val performancesRepository: PerformancesRepository
) : ViewModel() {

    var showPrompt by mutableStateOf(false)
        private set

    var serverUrlInput by mutableStateOf("")
        private set

    var isValidating by mutableStateOf(false)
        private set

    var validationError by mutableStateOf<ServerUrlValidationError?>(null)
        private set

    init {
        viewModelScope.launch {
            if (settingsRepository.serverUrl.first().isBlank()) {
                showPrompt = true
            }
        }
    }

    fun onServerUrlInputChanged(url: String) {
        serverUrlInput = url
        validationError = null
    }

    fun onConfirm() {
        val input = serverUrlInput.trim()
        if (!input.isValidServerUrlFormat()) {
            validationError = ServerUrlValidationError.INVALID_FORMAT
            return
        }
        validationError = null
        isValidating = true
        viewModelScope.launch {
            when (val result = serverValidationRepository.validate(input)) {
                is ApiResult.Success -> {
                    settingsRepository.saveServerUrl(input)
                    showPrompt = false
                    performancesRepository.loadPerformances()
                }
                is ApiResult.Error -> {
                    validationError = result.errorType.toServerUrlValidationError()
                }
            }
            isValidating = false
        }
    }
}
