package com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EditSetListEntryScreen(
    onSaved: () -> Unit,
    onSavedAsPending: (EditSetListEntryViewModel.PendingEntryResult) -> Unit = {},
    onCancel: () -> Unit,
    onNavigateToSearchWork: () -> Unit,
    onNavigateToSearchPerformer: () -> Unit,
    viewModel: EditSetListEntryViewModel = hiltViewModel()
) {
    when (viewModel.uiState) {
        is SetListEntryEditUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SetListEntryEditUiState.NotFound -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Set list entry not found", color = MaterialTheme.colorScheme.error)
            }
        }
        is SetListEntryEditUiState.Ready -> {
            EditSetListEntryForm(
                draftWorkTitle = viewModel.draftWorkTitle,
                draftFeaturedPerformers = viewModel.draftFeaturedPerformers,
                canSave = viewModel.canSave,
                isSaving = viewModel.isSaving,
                saveError = viewModel.saveError,
                onWorkClick = onNavigateToSearchWork,
                onAddPerformerClick = onNavigateToSearchPerformer,
                onUpdateFeaturedPerformerRole = viewModel::updateDraftFeaturedPerformerRole,
                onRemoveFeaturedPerformer = viewModel::removeDraftFeaturedPerformer,
                onMoveFeaturedPerformer = viewModel::moveDraftFeaturedPerformer,
                onSave = { viewModel.saveSetListEntry(onSaved, onSavedAsPending) },
                onCancel = onCancel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
