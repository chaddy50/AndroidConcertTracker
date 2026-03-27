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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import com.chaddy50.concerttracker.util.formatDate

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
                Text(
                    text = formatDate(performance.date),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = performance.venue.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (performance.conductor != null) {
                    Text(
                        text = "${performance.conductor.name}, conductor",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                text = performance.status.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// region Previews
private val previewVenue = Venue(id = "1", name = "Royal Albert Hall", osmId = 123456L)
private val previewConductor = Performer(id = "1", name = "Simon Rattle", type = PerformerType.CONDUCTOR)
private val previewPerformance = Performance(
    id = "1",
    date = "2024-11-15T19:30:00.000Z",
    venue = previewVenue,
    conductor = previewConductor,
    status = PerformanceStatus.ATTENDED
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