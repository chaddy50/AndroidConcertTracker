package com.chaddy50.concerttracker.navigation.topBarActions.syncStatusIndicator

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SyncStatusIndicator(viewModel: SyncStatusIndicatorViewModel = hiltViewModel()) {
    val status = viewModel.uiState.collectAsStateWithLifecycle().value
    if (!status.hasWork) return

    var isMenuExpanded by remember { mutableStateOf(false) }
    val tint = if (status.hasFailure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    TextButton(onClick = { isMenuExpanded = true }) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync status: ${status.queuedCount} queued",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = status.queuedCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = tint
        )
    }

    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
        Text(
            text = "Queued changes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        status.jobs.forEach { job -> SyncJobRow(job) }
        if (status.hasFailure) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Retry sync") },
                onClick = {
                    isMenuExpanded = false
                    viewModel.retry()
                }
            )
        }
    }
}
