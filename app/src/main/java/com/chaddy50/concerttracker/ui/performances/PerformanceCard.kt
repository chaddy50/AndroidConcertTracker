package com.chaddy50.concerttracker.ui.performances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.common.PerformerRow
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import com.chaddy50.concerttracker.util.formatDateTime

@Composable
fun PerformanceCard(performance: Performance, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column() {
                        Text(
                            text = formatDateTime(performance.date, LocalContext.current),
                            style = MaterialTheme.typography.titleMedium
                        )
                        for (performer in performance.performers) {
                            PerformerRow(performer)
                        }
                    }
                    Column {
                        Text(
                            text = performance.venue.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

            }
        }
    }
}

// region Previews
private val previewVenue = Venue(id = "1", name = "Overture Hall", osmId = "123456", osmType = "way")
private val previewConductor = Performer(id = "1", name = "John Demain", type = PerformerType.CONDUCTOR, specialty = "conductor")

private val previewPerformer1 = Performer(id = "2", name = "Madison Symphony Orchestra", type = PerformerType.ORCHESTRA)
private val previewPerformer2 = Performer(id = "3", name = "Time for Three", type = PerformerType.ENSEMBLE)
private val previewPerformance = Performance(
    id = "1",
    date = "2024-11-15T19:30:00.000Z",
    venue = previewVenue,
    conductor = previewConductor,
    status = PerformanceStatus.ATTENDED,
    performers = listOf(previewPerformer1, previewPerformer2, previewConductor)
)

@Preview(showBackground = true)
@Composable
fun PerformanceCardWithConductorPreview() {
    ConcertTrackerTheme {
        PerformanceCard(performance = previewPerformance, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PerformanceCardWithoutConductorPreview() {
    ConcertTrackerTheme {
        PerformanceCard(performance = previewPerformance.copy(conductor = null), onClick = {})
    }
}
// endregion