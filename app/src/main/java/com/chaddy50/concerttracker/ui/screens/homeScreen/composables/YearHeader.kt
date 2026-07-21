package com.chaddy50.concerttracker.ui.screens.homeScreen.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun YearHeader(year: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = year,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
private fun YearHeaderPreview() {
    ConcertTrackerTheme {
        Surface {
            YearHeader("2024")
        }
    }
}
// endregion
