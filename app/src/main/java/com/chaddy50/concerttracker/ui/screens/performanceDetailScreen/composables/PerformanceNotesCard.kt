package com.chaddy50.concerttracker.ui.screens.performanceDetailScreen.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.ui.composables.ExpandableNotesField
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun PerformanceNotesCard(
    draftNotes: String,
    onNotesChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { isEditing = !isEditing }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpandableNotesField(
                notes = draftNotes,
                label = stringResource(R.string.performance_notes_title),
                emptyText = stringResource(R.string.performance_notes_tap_to_add),
                isEditing = isEditing,
                onEditingChange = { isEditing = it },
                onNotesChange = onNotesChange
            )
        }
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun PerformanceNotesCardEmptyPreview() {
    ConcertTrackerTheme {
        PerformanceNotesCard(draftNotes = "", onNotesChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PerformanceNotesCardWithNotesPreview() {
    ConcertTrackerTheme {
        PerformanceNotesCard(
            draftNotes = "Went with Dad for his birthday. Seats were front row center.",
            onNotesChange = {}
        )
    }
}
// endregion
