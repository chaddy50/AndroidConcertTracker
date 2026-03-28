package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.entity.Composer
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.SetListEntryPerformer
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.common.PerformerRow
import com.chaddy50.concerttracker.ui.common.SectionHeader
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import com.chaddy50.concerttracker.util.formatDate

@Composable
fun PerformanceDetail(performance: Performance) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(
                text = formatDate(performance.date),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = performance.venue.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = performance.status.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (performance.performers.isNotEmpty()) {
            item {
                SectionHeader(title = "Performers")
            }
            items(performance.performers) { performer ->
                PerformerRow(performer.name)
            }
            if (performance.conductor != null) {
                item {
                    PerformerRow("${performance.conductor.name}, conductor")
                }
            }
        }

        if (performance.setList.isNotEmpty()) {
            item {
                SectionHeader(title = "Set List")
            }
            items(performance.setList.sortedBy { it.order }) { entry ->
                SetListEntryRow(
                    entry = entry,
                    performanceConductorId = performance.conductor?.id
                )
            }
        }
    }
}

// region Previews
private val previewConductor = Performer(id = "conductor-1", name = "Simon Rattle", type = PerformerType.CONDUCTOR)
private val previewSoloist = Performer(id = "soloist-1", name = "Martha Argerich", type = PerformerType.SOLO)
private val previewOrchestra = Performer(id = "orchestra-1", name = "London Symphony Orchestra", type = PerformerType.ORCHESTRA)
private val previewPerformance = Performance(
    id = "perf-1",
    date = "2024-11-15T19:30:00.000Z",
    venue = Venue(id = "venue-1", name = "Royal Albert Hall", osmId = "123456"),
    performers = listOf(previewOrchestra),
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
            featuredPerformers = listOf(SetListEntryPerformer(performer = previewSoloist, role = "Piano")),
            notes = "Exceptional performance of the second piano concerto."
        )
    )
)

@Preview(showBackground = true)
@Composable
fun PerformanceDetailPreview() {
    ConcertTrackerTheme {
        PerformanceDetail(performance = previewPerformance)
    }
}
// endregion