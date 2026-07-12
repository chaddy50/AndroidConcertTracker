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
    onDeleted: () -> Unit,
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
                isCreateMode = viewModel.isCreateMode,
                draftWorkTitle = viewModel.draftWorkTitle,
                draftOrder = viewModel.draftOrder,
                draftFeaturedPerformers = viewModel.draftFeaturedPerformers,
                canSave = viewModel.canSave,
                isSaving = viewModel.isSaving,
                isDeleting = viewModel.isDeleting,
                saveError = viewModel.saveError,
                onWorkClick = onNavigateToSearchWork,
                onDraftOrderChange = viewModel::updateDraftOrder,
                onAddPerformerClick = onNavigateToSearchPerformer,
                onUpdateFeaturedPerformerRole = viewModel::updateDraftFeaturedPerformerRole,
                onRemoveFeaturedPerformer = viewModel::removeDraftFeaturedPerformer,
                onSave = { viewModel.saveSetListEntry(onSaved, onSavedAsPending) },
                onCancel = onCancel,
                onDelete = { viewModel.deleteSetListEntry(onDeleted) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
