package com.chaddy50.concerttracker.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun PerformerRow(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

// region Previews
@Preview(showBackground = true)
@Composable
fun PerformerRowPreview() {
    ConcertTrackerTheme {
        PerformerRow(name = "London Symphony Orchestra")
    }
}

@Preview(showBackground = true)
@Composable
fun PerformerRowConductorPreview() {
    ConcertTrackerTheme {
        PerformerRow(name = "Simon Rattle, conductor")
    }
}
// endregion