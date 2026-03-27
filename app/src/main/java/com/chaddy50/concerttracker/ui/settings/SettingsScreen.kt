package com.chaddy50.concerttracker.ui.settings

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    SettingsContent(
        serverUrl = viewModel.serverUrl,
        onServerUrlChanged = viewModel::onServerUrlChanged
    )
}
