package com.chaddy50.concerttracker.ui.setListEntryEdit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SetListEntryEditScreen(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    onNavigateToSearchWork: () -> Unit,
    onNavigateToSearchConductor: () -> Unit,
    onNavigateToSearchPerformer: () -> Unit,
    pendingWorkId: String?,
    pendingWorkName: String?,
    pendingWorkComposerId: String?,
    pendingWorkComposerName: String?,
    pendingConductorId: String?,
    pendingConductorName: String?,
    pendingPerformerId: String?,
    pendingPerformerName: String?,
    pendingPerformerType: String?,
    pendingPerformerSpecialty: String?,
    viewModel: SetListEntryEditViewModel = hiltViewModel()
) {
    LaunchedEffect(pendingWorkId, pendingWorkName, pendingWorkComposerId, pendingWorkComposerName) {
        if (pendingWorkId != null && pendingWorkName != null &&
            pendingWorkComposerId != null && pendingWorkComposerName != null) {
            viewModel.selectWork(pendingWorkId, pendingWorkName, pendingWorkComposerId, pendingWorkComposerName)
        }
    }

    LaunchedEffect(pendingConductorId, pendingConductorName) {
        if (pendingConductorId != null && pendingConductorName != null) {
            viewModel.updateDraftConductor(pendingConductorId, pendingConductorName)
        }
    }

    LaunchedEffect(pendingPerformerId, pendingPerformerName) {
        if (pendingPerformerId != null && pendingPerformerName != null) {
            viewModel.addDraftFeaturedPerformer(
                pendingPerformerId,
                pendingPerformerName,
                pendingPerformerType,
                pendingPerformerSpecialty
            )
        }
    }

    when (val state = viewModel.uiState) {
        is SetListEntryEditUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SetListEntryEditUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = viewModel::loadData,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is SetListEntryEditUiState.Ready -> {
            SetListEntryEditForm(
                isCreateMode = viewModel.isCreateMode,
                draftWorkTitle = viewModel.draftWorkTitle,
                draftOrder = viewModel.draftOrder,
                draftConductorName = viewModel.draftConductorName,
                draftFeaturedPerformers = viewModel.draftFeaturedPerformers,
                canSave = viewModel.canSave,
                isSaving = viewModel.isSaving,
                isDeleting = viewModel.isDeleting,
                saveError = viewModel.saveError,
                onWorkClick = onNavigateToSearchWork,
                onDraftOrderChange = viewModel::updateDraftOrder,
                onConductorClick = onNavigateToSearchConductor,
                onClearConductor = viewModel::clearDraftConductor,
                onAddPerformerClick = onNavigateToSearchPerformer,
                onUpdateFeaturedPerformerRole = viewModel::updateDraftFeaturedPerformerRole,
                onRemoveFeaturedPerformer = viewModel::removeDraftFeaturedPerformer,
                onSave = { viewModel.saveSetListEntry(onSaved) },
                onCancel = onCancel,
                onDelete = { viewModel.deleteSetListEntry(onDeleted) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
