package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.ui.composables.SaveCancelButtons

@Composable
fun EditPerformanceScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    onNavigateToCreateVenue: () -> Unit,
    onNavigateToSearchPerformer: () -> Unit,
    onNavigateToAddSetListEntry: () -> Unit,
    onNavigateToEditSetListEntry: (entryId: String) -> Unit,
    onNavigateToEditPendingSetListEntry: (localId: String) -> Unit,
    viewModel: EditPerformanceViewModel = hiltViewModel()
) {
    when (viewModel.uiState) {
        is PerformanceEditUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PerformanceEditUiState.NotFound -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Performance not found", color = MaterialTheme.colorScheme.error)
            }
        }
        is PerformanceEditUiState.Ready -> {
            Column(modifier = Modifier.fillMaxSize()) {
                PerformanceEditForm(
                    draftDate = viewModel.draftDate,
                    draftVenueName = viewModel.draftVenueName,
                    draftPerformers = viewModel.draftPerformers,
                    currentSetList = viewModel.currentSetList,
                    pendingSetListEntries = viewModel.pendingSetListEntries,
                    onDraftDateChange = viewModel::updateDraftDate,
                    onVenueClick = onNavigateToCreateVenue,
                    onAddPerformerClick = onNavigateToSearchPerformer,
                    onRemovePerformer = viewModel::removeDraftPerformer,
                    onMovePerformer = viewModel::moveDraftPerformer,
                    onAddSetListEntryClick = onNavigateToAddSetListEntry,
                    onEditSetListEntryClick = onNavigateToEditSetListEntry,
                    onEditPendingSetListEntryClick = onNavigateToEditPendingSetListEntry,
                    onMoveSetListEntry = viewModel::moveSetListEntry,
                    onMovePendingSetListEntry = viewModel::movePendingSetListEntry,
                    modifier = Modifier.weight(1f)
                )
                SaveCancelButtons(
                    onCancel = onCancel,
                    onSave = { viewModel.savePerformance(onSaved) },
                    canSave = viewModel.canSave,
                    isSaving = viewModel.isSaving
                )
                if (viewModel.saveError != null) {
                    Text(
                        text = viewModel.saveError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
