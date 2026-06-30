package com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.external.api.MusicBrainzResult
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.data.enum.PerformerType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomMusicBrainzEntityCreationDialog(
    entityType: MusicBrainzEntityType,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (MusicBrainzResult) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var specialty by remember { mutableStateOf("") }
    var selectedPerformerType by remember { mutableStateOf(PerformerType.entries.first()) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val title = when (entityType) {
                MusicBrainzEntityType.PERFORMER -> "Create custom performer"
                MusicBrainzEntityType.CONDUCTOR -> "Create custom conductor"
                MusicBrainzEntityType.COMPOSER -> "Create custom composer"
            }
            Text(title)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (entityType == MusicBrainzEntityType.PERFORMER) {
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedPerformerType.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            PerformerType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            type.name
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        )
                                    },
                                    onClick = {
                                        selectedPerformerType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("Specialty (optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        MusicBrainzResult(
                            id = "CUSTOM-${UUID.randomUUID()}",
                            name = name.trim(),
                            description = specialty.trim().ifBlank { null },
                            performerType = if (entityType == MusicBrainzEntityType.PERFORMER) {
                                selectedPerformerType
                            } else {
                                null
                            }
                        )
                    )
                },
                enabled = name.isNotBlank()
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
