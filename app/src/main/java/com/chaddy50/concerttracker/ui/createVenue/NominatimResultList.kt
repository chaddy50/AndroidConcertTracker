package com.chaddy50.concerttracker.ui.createVenue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chaddy50.concerttracker.data.entity.NominatimResult
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun NominatimResultList(
    results: List<NominatimResult>,
    isEnabled: Boolean,
    onResultClick: (NominatimResult) -> Unit
) {
    LazyColumn {
        items(results) { result ->
            ListItem(
                headlineContent = { Text(result.name) },
                supportingContent = { Text(result.displayName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) { onResultClick(result) }
            )
            HorizontalDivider()
        }
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun NominatimResultListPreview() {
    ConcertTrackerTheme {
        NominatimResultList(
            results = listOf(
                NominatimResult(
                    osmId = 12345L,
                    osmType = "way",
                    name = "Symphony Hall",
                    displayName = "Symphony Hall, 301, Massachusetts Avenue, Back Bay, Boston, United States"
                ),
                NominatimResult(
                    osmId = 67890L,
                    osmType = "node",
                    name = "Boston Symphony Orchestra",
                    displayName = "Boston Symphony Orchestra, Huntington Avenue, Boston, United States"
                )
            ),
            isEnabled = true,
            onResultClick = {}
        )
    }
}
// endregion