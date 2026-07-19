package com.chaddy50.concerttracker.ui.screens.homeScreen.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.domain.Performance
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.composables.PerformerRow
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = formatDateTime(performance.date, LocalContext.current),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = performance.venue.name + ", " + performance.venue.city,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            for (performer in performance.performers) {
                PerformerRow(performer)
            }
        }
    }
}

// region Previews
private val previewVenue = Venue(id = "1", name = "Overture Hall", osmId = "123456", osmType = "way", city = "Madison")
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