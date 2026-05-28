package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun CustomComposerCreationDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (composerName: String) -> Unit
) {
    var composerName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create custom composer") },
        text = {
            OutlinedTextField(
                value = composerName,
                onValueChange = { composerName = it },
                label = { Text("Composer name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(composerName.trim()) },
                enabled = composerName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
