package com.chaddy50.concerttracker.ui.performanceEdit

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
fun PerformanceEditScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PerformanceEditViewModel = hiltViewModel()
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
                    draftStatus = viewModel.draftStatus,
                    onDraftDateChange = viewModel::updateDraftDate,
                    onDraftStatusChange = viewModel::updateDraftStatus,
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
            }
        }
    }
}