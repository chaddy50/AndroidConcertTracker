package com.chaddy50.concerttracker.ui.screens.settingsScreen.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chaddy50.concerttracker.R

@Composable
fun SyncSection(viewModel: SyncSectionViewModel = hiltViewModel()) {
    val status = viewModel.uiState.collectAsStateWithLifecycle().value
    var pendingDiscardJobId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = stringResource(R.string.sync_section_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (!status.hasWork) {
            Text(
                text = stringResource(R.string.sync_all_synced),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            return@Column
        }

        status.jobs.forEach { job -> SyncJobRow(job, onDiscard = { pendingDiscardJobId = it }) }

        if (status.hasFailure) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = viewModel::retry) {
                    Text(stringResource(R.string.sync_retry))
                }
            }
        }
    }

    pendingDiscardJobId?.let { jobId ->
        AlertDialog(
            onDismissRequest = { pendingDiscardJobId = null },
            title = { Text(stringResource(R.string.sync_discard_dialog_title)) },
            text = { Text(stringResource(R.string.sync_discard_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discard(jobId)
                    pendingDiscardJobId = null
                }) {
                    Text(stringResource(R.string.sync_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDiscardJobId = null }) {
                    Text(stringResource(R.string.sync_discard_cancel))
                }
            }
        )
    }
}
