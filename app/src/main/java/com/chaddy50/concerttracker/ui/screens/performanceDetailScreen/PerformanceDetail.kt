package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.domain.Composer
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.domain.FeaturedPerformer
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.domain.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.composables.PerformerRow
import com.chaddy50.concerttracker.ui.screens.performanceDetailScreen.composables.PerformanceNotesCard
import com.chaddy50.concerttracker.ui.screens.performanceDetailScreen.composables.SetListEntryCard
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import com.chaddy50.concerttracker.util.formatDate
import com.chaddy50.concerttracker.util.formatTime

private val GROUP_TYPES = setOf(PerformerType.ORCHESTRA, PerformerType.ENSEMBLE, PerformerType.CHORUS)

@Composable
fun PerformanceDetail(
    performance: Performance,
    draftNotes: Map<String, String>,
    onDraftNoteChange: (entryId: String, notes: String) -> Unit,
    draftPerformanceNotes: String,
    onPerformanceNotesChange: (notes: String) -> Unit,
    didSavingNotesHaveError: Boolean = false
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(performance.date, LocalContext.current),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = performance.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(performance.date, LocalContext.current),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = performance.venue.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        val hasPerformers = performance.performers.isNotEmpty() || performance.conductor != null
        if (hasPerformers) {
            val groups = performance.performers.filter { it.type in GROUP_TYPES }
            val soloists = performance.performers.filter { it.type !in GROUP_TYPES }
            items(groups) { performer ->
                PerformerRow(performer)
            }
            items(soloists) { soloist ->
                PerformerRow(soloist)
            }
            if (performance.conductor != null) {
                item {
                    PerformerRow(performance.conductor)
                }
            }
        }

        item {
            PerformanceNotesCard(
                draftNotes = draftPerformanceNotes,
                onNotesChange = onPerformanceNotesChange
            )
        }

        if (performance.setList.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(top= 8.dp, bottom=8.dp))
                if (didSavingNotesHaveError) {
                    Text(
                        text = "Failed to save notes",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            items(performance.setList.sortedBy { it.order }) { entry ->
                SetListEntryCard(
                    entry = entry,
                    performanceConductorId = performance.conductor?.id,
                    draftNotes = draftNotes[entry.id] ?: "",
                    onNotesChange = { onDraftNoteChange(entry.id, it) }
                )
            }
        }
    }
}

// region Previews
private val previewConductor = Performer(id = "conductor-1", name = "Simon Rattle", type = PerformerType.CONDUCTOR, specialty = "conductor")
private val previewSoloist = Performer(id = "soloist-1", name = "Martha Argerich", type = PerformerType.SOLO, specialty = "pianist")
private val previewOrchestra = Performer(id = "orchestra-1", name = "London Symphony Orchestra", type = PerformerType.ORCHESTRA)
private val previewPerformance = Performance(
    id = "perf-1",
    date = "2024-11-15T19:30:00.000Z",
    venue = Venue(id = "venue-1", name = "Royal Albert Hall", osmId = "123456", osmType = "way"),
    performers = listOf(previewOrchestra, previewSoloist),
    conductor = previewConductor,
    status = PerformanceStatus.ATTENDED,
    setList = listOf(
        SetListEntry(
            id = "entry-1",
            work = Work(
                id = "work-1",
                title = "Piano Concerto No. 2 in B flat major",
                composers = listOf(Composer(id = "composer-1", name = "Johannes Brahms"))
            ),
            order = 1,
            conductor = null,
            featuredPerformers = listOf(FeaturedPerformer(performer = previewSoloist, role = "Piano")),
            notes = "Exceptional performance of the second piano concerto."
        )
    ),
    notes = "Went with Dad for his birthday."
)

@Preview(showBackground = true)
@Composable
fun PerformanceDetailPreview() {
    ConcertTrackerTheme {
        PerformanceDetail(
            performance = previewPerformance,
            draftNotes = mapOf("entry-1" to "Exceptional performance of the second piano concerto."),
            onDraftNoteChange = { _, _ -> },
            draftPerformanceNotes = "Went with Dad for his birthday.",
            onPerformanceNotesChange = {}
        )
    }
}
// endregion
