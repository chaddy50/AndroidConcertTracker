package com.chaddy50.concerttracker.ui.screens.settingsScreen

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.R

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    BackHandler { viewModel.onAttemptExit() }

    LaunchedEffect(viewModel.exitApproved) {
        if (viewModel.exitApproved) onNavigateBack()
    }

    SettingsContent(
        serverUrl = viewModel.serverUrl,
        isValidating = viewModel.isValidating,
        validationError = viewModel.validationError,
        onServerUrlChanged = viewModel::onServerUrlChanged
    )

    if (viewModel.showInvalidExitDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onKeepEditing,
            title = { Text(stringResource(R.string.settings_invalid_url_dialog_title)) },
            text = { Text(stringResource(R.string.settings_invalid_url_dialog_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onKeepEditing) {
                    Text(stringResource(R.string.settings_invalid_url_keep_editing))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDiscardChanges) {
                    Text(stringResource(R.string.settings_invalid_url_discard))
                }
            }
        )
    }
}
