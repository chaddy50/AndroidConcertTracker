package com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.repository.PerformancesRepository
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerUrlPromptViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val performancesRepository: PerformancesRepository
) : ViewModel() {

    var showPrompt by mutableStateOf(false)
        private set

    var serverUrlInput by mutableStateOf("")
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
    }

    fun onConfirm() {
        viewModelScope.launch {
            settingsRepository.saveServerUrl(serverUrlInput.trim())
            showPrompt = false
            performancesRepository.loadPerformances()
        }
    }
}
