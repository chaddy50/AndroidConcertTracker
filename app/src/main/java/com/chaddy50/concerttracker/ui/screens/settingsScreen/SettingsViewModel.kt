package com.chaddy50.concerttracker.ui.screens.settingsScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.ServerUrlValidationError
import com.chaddy50.concerttracker.data.repository.ServerValidationRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import com.chaddy50.concerttracker.data.repository.isValidServerUrlFormat
import com.chaddy50.concerttracker.data.repository.toServerUrlValidationError
import com.chaddy50.concerttracker.dependencyInjection.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val serverValidationRepository: ServerValidationRepository,
    private val performancesRepository: PerformancesRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    var serverUrl by mutableStateOf("")
        private set

    var isValidating by mutableStateOf(false)
        private set

    var validationError by mutableStateOf<ServerUrlValidationError?>(null)
        private set

    var showInvalidExitDialog by mutableStateOf(false)
        private set

    var exitApproved by mutableStateOf(false)
        private set

    private var loadedUrl: String = ""

    private var lastValidatedInput: String? = null
    private var lastValidationValid: Boolean = false

    init {
        viewModelScope.launch {
            loadedUrl = settingsRepository.serverUrl.first()
            serverUrl = loadedUrl
            record(loadedUrl, true)
        }
        viewModelScope.launch {
            snapshotFlow { serverUrl }
                .drop(1) // skip the seed emission
                .debounce(600)
                .collectLatest { validate(it.trim()) }
        }
    }

    fun onServerUrlChanged(url: String) {
        serverUrl = url
        validationError = null
    }

    fun onAttemptExit() {
        val trimmed = serverUrl.trim()
        if (trimmed == loadedUrl) {
            exitApproved = true
            return
        }
        if (trimmed == lastValidatedInput) {
            if (lastValidationValid) commitAndExit(trimmed) else showInvalidExitDialog = true
            return
        }
        viewModelScope.launch {
            if (validate(trimmed)) commitAndExit(trimmed) else showInvalidExitDialog = true
        }
    }

    fun onKeepEditing() {
        showInvalidExitDialog = false
    }

    fun onDiscardChanges() {
        serverUrl = loadedUrl
        showInvalidExitDialog = false
        exitApproved = true
    }

    private suspend fun validate(input: String): Boolean {
        if (input == loadedUrl) {
            validationError = null
            record(input, true)
            return true
        }
        if (!input.isValidServerUrlFormat()) {
            validationError = ServerUrlValidationError.INVALID_FORMAT
            record(input, false)
            return false
        }
        isValidating = true
        val valid = when (val result = serverValidationRepository.validate(input)) {
            is ApiResult.Success -> {
                validationError = null
                true
            }
            is ApiResult.Error -> {
                validationError = result.errorType.toServerUrlValidationError()
                false
            }
        }
        isValidating = false
        record(input, valid)
        return valid
    }

    private fun commitAndExit(url: String) {
        viewModelScope.launch {
            settingsRepository.saveServerUrl(url)
            applicationScope.launch { performancesRepository.loadPerformances() }
            exitApproved = true
        }
    }

    private fun record(input: String, valid: Boolean) {
        lastValidatedInput = input
        lastValidationValid = valid
    }
}
