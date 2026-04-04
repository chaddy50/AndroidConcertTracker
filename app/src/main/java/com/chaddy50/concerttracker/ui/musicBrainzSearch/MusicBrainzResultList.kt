package com.chaddy50.concerttracker.ui.musicBrainzSearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chaddy50.concerttracker.data.entity.MusicBrainzResult
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun MusicBrainzResultList(
    results: List<MusicBrainzResult>,
    onResultClick: (MusicBrainzResult) -> Unit
) {
    LazyColumn {
        items(results) { result ->
            ListItem(
                headlineContent = { Text(result.name) },
                supportingContent = result.description?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResultClick(result) }
            )
            HorizontalDivider()
        }
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun MusicBrainzResultListPreview() {
    ConcertTrackerTheme {
        MusicBrainzResultList(
            results = listOf(
                MusicBrainzResult(
                    id = "abc123",
                    name = "Boston Symphony Orchestra",
                    description = "American orchestra based in Boston"
                ),
                MusicBrainzResult(
                    id = "def456",
                    name = "London Symphony Orchestra",
                    description = null
                )
            ),
            onResultClick = {}
        )
    }
}
// endregion
