package com.chaddy50.concerttracker.ui.setListEntryEdit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SetListEntryEditForm(
    isCreateMode: Boolean,
    draftWorkTitle: String?,
    draftOrder: String,
    draftConductorName: String?,
    draftFeaturedPerformers: List<DraftFeaturedPerformer>,
    canSave: Boolean,
    isSaving: Boolean,
    isDeleting: Boolean,
    saveError: String?,
    onWorkClick: () -> Unit,
    onDraftOrderChange: (String) -> Unit,
    onConductorClick: () -> Unit,
    onClearConductor: () -> Unit,
    onAddPerformerClick: () -> Unit,
    onUpdateFeaturedPerformerRole: (performerId: String, role: String) -> Unit,
    onRemoveFeaturedPerformer: (performerId: String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val workInteractionSource = remember { MutableInteractionSource() }
            val isWorkPressed by workInteractionSource.collectIsPressedAsState()
            if (isWorkPressed) onWorkClick()

            OutlinedTextField(
                value = draftWorkTitle ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Work") },
                interactionSource = workInteractionSource,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = draftOrder,
                onValueChange = onDraftOrderChange,
                label = { Text("Order") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            val conductorInteractionSource = remember { MutableInteractionSource() }
            val isConductorPressed by conductorInteractionSource.collectIsPressedAsState()
            if (isConductorPressed) onConductorClick()

            OutlinedTextField(
                value = draftConductorName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Conductor (optional)") },
                trailingIcon = {
                    if (draftConductorName != null) {
                        IconButton(onClick = onClearConductor) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear conductor"
                            )
                        }
                    }
                },
                interactionSource = conductorInteractionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Text(
                text = "Featured Performers",
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            draftFeaturedPerformers.forEach { draftPerformer ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draftPerformer.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = draftPerformer.role,
                            onValueChange = { onUpdateFeaturedPerformerRole(draftPerformer.performerId, it) },
                            label = { Text("Role") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = { onRemoveFeaturedPerformer(draftPerformer.performerId) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove performer"
                        )
                    }
                }
            }
            TextButton(
                onClick = onAddPerformerClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Add Performer")
            }
        }

        if (saveError != null) {
            Text(
                text = saveError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isCreateMode) {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !isSaving && !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                }
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving && !isDeleting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                enabled = canSave && !isSaving && !isDeleting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete entry?") },
            text = { Text("This will permanently remove this work from the set list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
