package com.chaddy50.concerttracker.ui.screens.editPerformerScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.navigation.routes.PerformerUpdatedResult

@Composable
fun EditPerformerScreen(
    onSaved: (PerformerUpdatedResult) -> Unit,
    onCancel: () -> Unit,
    viewModel: EditPerformerViewModel = hiltViewModel()
) {
    when (viewModel.uiState) {
        is PerformerEditUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PerformerEditUiState.NotFound -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Performer not found", color = MaterialTheme.colorScheme.error)
            }
        }
        is PerformerEditUiState.Ready -> {
            EditPerformerForm(
                draftName = viewModel.draftName,
                draftType = viewModel.draftType,
                draftSpecialty = viewModel.draftSpecialty,
                isNameEditable = viewModel.isNameEditable,
                canSave = viewModel.canSave,
                isSaving = viewModel.isSaving,
                saveError = viewModel.saveError,
                onEnableNameEditing = viewModel::enableNameEditing,
                onNameChange = viewModel::updateDraftName,
                onTypeChange = viewModel::updateDraftType,
                onSpecialtyChange = viewModel::updateDraftSpecialty,
                onSave = { viewModel.savePerformer(onSaved) },
                onCancel = onCancel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
