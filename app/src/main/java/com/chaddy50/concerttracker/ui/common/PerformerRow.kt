package com.chaddy50.concerttracker.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun PerformerRow(performer: Performer, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    val displayName = if (performer.specialty != null) "${performer.name}, ${performer.specialty}" else performer.name

    Text(
        text = displayName,
        style = style,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

// region Previews
@Preview(showBackground = true)
@Composable
fun PerformerRowPreview() {
    ConcertTrackerTheme {
        PerformerRow(Performer(id="1", name="London Symphony Orchestra", type= PerformerType.ORCHESTRA))
    }
}

@Preview(showBackground = true)
@Composable
fun PerformerRowConductorPreview() {
    ConcertTrackerTheme {
        PerformerRow(Performer(id="1", name="Simon Rattle", type=PerformerType.CONDUCTOR, specialty = "conductor"))
    }
}
// endregion