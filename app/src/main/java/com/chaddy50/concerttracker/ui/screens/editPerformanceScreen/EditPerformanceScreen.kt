package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
    when (val state = viewModel.uiState) {
        is PerformanceEditUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PerformanceEditUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = viewModel::loadPerformance,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is PerformanceEditUiState.Ready -> {
            Column(modifier = Modifier.fillMaxSize()) {
                PerformanceEditForm(
                    draftDate = viewModel.draftDate,
                    draftVenueName = viewModel.draftVenueName,
                    draftPerformers = viewModel.draftPerformers,
                    draftStatus = viewModel.draftStatus,
                    currentSetList = viewModel.currentSetList,
                    pendingSetListEntries = viewModel.pendingSetListEntries,
                    onDraftDateChange = viewModel::updateDraftDate,
                    onDraftStatusChange = viewModel::updateDraftStatus,
                    onVenueClick = onNavigateToCreateVenue,
                    onAddPerformerClick = onNavigateToSearchPerformer,
                    onRemovePerformer = viewModel::removeDraftPerformer,
                    onAddSetListEntryClick = onNavigateToAddSetListEntry,
                    onEditSetListEntryClick = onNavigateToEditSetListEntry,
                    onEditPendingSetListEntryClick = onNavigateToEditPendingSetListEntry,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !viewModel.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { viewModel.savePerformance(onSaved) },
                        enabled = viewModel.canSave && !viewModel.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
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
