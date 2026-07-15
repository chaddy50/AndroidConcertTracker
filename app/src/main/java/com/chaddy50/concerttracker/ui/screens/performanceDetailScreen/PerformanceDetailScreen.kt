package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PerformanceDetailScreen(viewModel: PerformanceDetailViewModel = hiltViewModel()) {
    when (val state = viewModel.uiState.collectAsStateWithLifecycle().value) {
        is PerformanceDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PerformanceDetailUiState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Performance not found")
            }
        }
        is PerformanceDetailUiState.Content -> {
            PerformanceDetail(
                performance = state.performance,
                draftNotes = viewModel.draftNotes,
                onDraftNoteChange = viewModel::updateDraftNote,
                draftPerformanceNotes = viewModel.draftPerformanceNotes,
                onPerformanceNotesChange = viewModel::updateDraftPerformanceNotes,
                didSavingNotesHaveError = viewModel.didSavingNotesHaveError != null
            )
        }
    }
}
