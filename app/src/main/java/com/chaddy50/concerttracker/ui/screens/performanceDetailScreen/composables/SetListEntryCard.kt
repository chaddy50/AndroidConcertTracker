package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.domain.FeaturedPerformer
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.composables.NotesDialog
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun SetListEntryCard(
    entry: SetListEntry,
    performanceConductorId: String?,
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
            SetListEntryHeader(entry = entry, performanceConductorId = performanceConductorId)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                if (draftNotes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.set_list_entry_your_notes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = draftNotes,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = stringResource(R.string.set_list_entry_tap_to_add_notes),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (isSheetOpen) {
        NotesDialog(
            header = {
                SetListEntryHeader(entry = entry, performanceConductorId = performanceConductorId)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
private val previewConductor = Performer(id = "conductor-1", name = "Simon Rattle", type = PerformerType.CONDUCTOR, specialty = "conductor")
private val previewSoloist = Performer(id = "soloist-1", name = "Martha Argerich", type = PerformerType.SOLO)
private val previewWork = Work(
    id = "work-1",
    title = "Piano Concerto No. 2",
    composers = listOf(Composer(id = "composer-1", name = "Johannes Brahms"))
)
private val previewEntry = SetListEntry(
    id = "entry-1",
    work = previewWork,
    order = 1,
    conductor = previewConductor,
    featuredPerformers = listOf(FeaturedPerformer(performer = previewSoloist, role = "Piano"))
)

@Preview(showBackground = true)
@Composable
fun SetListEntryCardPreview() {
    ConcertTrackerTheme {
        SetListEntryCard(
            entry = previewEntry,
            performanceConductorId = "conductor-2",
            draftNotes = "",
            onNotesChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SetListEntryCardWithNotesPreview() {
    ConcertTrackerTheme {
        SetListEntryCard(
            entry = previewEntry,
            performanceConductorId = "conductor-1",
            draftNotes = "Argerich's tempo in the first movement was breathtaking.",
            onNotesChange = {}
        )
    }
}
// endregion