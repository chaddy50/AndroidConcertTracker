package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chaddy50.concerttracker.data.entity.Composer
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.FeaturedPerformer
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.composables.PerformerRow
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
internal fun SetListEntryHeader(entry: SetListEntry, performanceConductorId: String?) {
    Text(
        text = entry.work.title,
        style = MaterialTheme.typography.titleMedium
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
        PerformerRow(performer = entry.conductor)
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
    featuredPerformers = listOf(FeaturedPerformer(performer = previewSoloist, role = "Piano")),
    notes = null
)

@Preview(showBackground = true)
@Composable
fun SetListEntryHeaderPreview() {
    ConcertTrackerTheme {
        SetListEntryHeader(entry = previewEntry, performanceConductorId = "conductor-2")
    }
}
// endregion