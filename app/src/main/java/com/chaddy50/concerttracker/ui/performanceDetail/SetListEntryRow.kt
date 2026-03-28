package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.entity.Composer
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.SetListEntryPerformer
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun SetListEntryRow(entry: SetListEntry, performanceConductorId: String?) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = "${entry.order}. ${entry.work.title}",
            style = MaterialTheme.typography.bodyLarge
        )
        val composerNames = entry.work.composers.joinToString(", ") { it.name }
        if (composerNames.isNotEmpty()) {
            Text(
                text = composerNames,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (entry.conductor != null && entry.conductor.id != performanceConductorId) {
            Text(
                text = "${entry.conductor.name}, conductor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (entry.notes != null) {
            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        entry.featuredPerformers.forEach { featuredPerformer ->
            val label = if (featuredPerformer.role != null) {
                "${featuredPerformer.performer.name}, ${featuredPerformer.role}"
            } else {
                featuredPerformer.performer.name
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// region Previews
private val previewConductor = Performer(id = "conductor-1", name = "Simon Rattle", type = PerformerType.CONDUCTOR)
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
    featuredPerformers = listOf(SetListEntryPerformer(performer = previewSoloist, role = "Piano")),
    notes = null
)
private val previewEntryWithNotes = previewEntry.copy(
    notes = "Argerich's tempo in the first movement was breathtaking."
)

@Preview(showBackground = true)
@Composable
fun SetListEntryRowWithDifferentConductorPreview() {
    ConcertTrackerTheme {
        SetListEntryRow(entry = previewEntry, performanceConductorId = "conductor-2")
    }
}

@Preview(showBackground = true)
@Composable
fun SetListEntryRowWithSameConductorPreview() {
    ConcertTrackerTheme {
        SetListEntryRow(entry = previewEntry, performanceConductorId = "conductor-1")
    }
}

@Preview(showBackground = true)
@Composable
fun SetListEntryRowWithNotesPreview() {
    ConcertTrackerTheme {
        SetListEntryRow(entry = previewEntryWithNotes, performanceConductorId = "conductor-1")
    }
}
// endregion