package com.chaddy50.concerttracker.ui.screens.editPerformerScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.composables.SaveCancelButtons
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPerformerForm(
    draftName: String,
    draftType: PerformerType,
    draftSpecialty: String?,
    isNameEditable: Boolean,
    canSave: Boolean,
    isSaving: Boolean,
    saveError: String?,
    onEnableNameEditing: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (PerformerType) -> Unit,
    onSpecialtyChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    enabled = isNameEditable,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (!isNameEditable) {
                    IconButton(onClick = onEnableNameEditing) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit name"
                        )
                    }
                }
            }

            var typeDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = draftType.name.lowercase().replaceFirstChar { it.uppercase() },
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
                            text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onTypeChange(type)
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draftSpecialty ?: "",
                onValueChange = onSpecialtyChange,
                label = { Text("Specialty") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        if (saveError != null) {
            Text(
                text = saveError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        SaveCancelButtons(
            onCancel = onCancel,
            onSave = onSave,
            canSave = canSave,
            isSaving = isSaving
        )
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun EditPerformerFormPreview() {
    ConcertTrackerTheme {
        EditPerformerForm(
            draftName = "Yo-Yo Ma",
            draftType = PerformerType.SOLO,
            draftSpecialty = "Cellist",
            isNameEditable = false,
            canSave = true,
            isSaving = false,
            saveError = null,
            onEnableNameEditing = {},
            onNameChange = {},
            onTypeChange = {},
            onSpecialtyChange = {},
            onSave = {},
            onCancel = {}
        )
    }
}
// endregion
