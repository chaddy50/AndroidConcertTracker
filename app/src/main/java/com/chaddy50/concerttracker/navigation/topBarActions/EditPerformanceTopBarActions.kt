package com.chaddy50.concerttracker.navigation.topBarActions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun EditPerformanceTopBarActions(
    onDeletePerformance: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    IconButton(onClick = { showConfirmDialog = true }) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete performance",
            tint = MaterialTheme.colorScheme.error
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            text = { Text("Are you sure you want to delete this performance?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onDeletePerformance()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}