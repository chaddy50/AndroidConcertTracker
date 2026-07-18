package com.chaddy50.concerttracker.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SaveCancelButtons(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
        Button(
            onClick = onSave,
            enabled = canSave && !isSaving,
            modifier = Modifier.weight(1f)
        ) {
            Text("Save")
        }
    }
}
