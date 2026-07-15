package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.ui.composables.NotesDialog
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun PerformanceNotesCard(
    draftNotes: String,
    onNotesChange: (String) -> Unit
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    var editingNotes by remember { mutableStateOf(TextFieldValue(draftNotes)) }

    LaunchedEffect(isSheetOpen) {
        if (isSheetOpen) {
            editingNotes = TextFieldValue(draftNotes, selection = TextRange(draftNotes.length))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { isSheetOpen = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.performance_notes_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (draftNotes.isNotEmpty()) {
                Text(
                    text = draftNotes,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = stringResource(R.string.performance_notes_tap_to_add),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (isSheetOpen) {
        NotesDialog(
            header = {
                Text(
                    text = stringResource(R.string.performance_notes_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            label = stringResource(R.string.performance_form_notes_label),
            editingNotes = editingNotes,
            onNotesChange = { editingNotes = it },
            onDismiss = {
                onNotesChange(editingNotes.text)
                isSheetOpen = false
            }
        )
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun PerformanceNotesCardEmptyPreview() {
    ConcertTrackerTheme {
        PerformanceNotesCard(draftNotes = "", onNotesChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PerformanceNotesCardWithNotesPreview() {
    ConcertTrackerTheme {
        PerformanceNotesCard(
            draftNotes = "Went with Dad for his birthday. Seats were front row center.",
            onNotesChange = {}
        )
    }
}
// endregion
