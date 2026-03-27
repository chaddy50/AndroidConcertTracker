package com.chaddy50.concerttracker.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    var serverUrl by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            serverUrl = repository.serverUrl.first()
        }
    }

    fun onServerUrlChanged(url: String) {
        serverUrl = url
        viewModelScope.launch {
            repository.saveServerUrl(url)
        }
    }
}
