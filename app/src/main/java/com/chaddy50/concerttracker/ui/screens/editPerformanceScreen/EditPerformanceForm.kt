package com.chaddy50.concerttracker.ui.screens.editPerformanceScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.domain.SetListEntry
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.composables.DatePickerField
import com.chaddy50.concerttracker.ui.composables.TimePickerField
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.fields.performerList.PerformerEditList
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.fields.setList.SetListEditList
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.fields.VenueField
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun PerformanceEditForm(
    draftDate: Long?,
    draftVenueName: String?,
    draftPerformers: List<Performer>,
    currentSetList: List<SetListEntry>,
    pendingSetListEntries: List<PendingSetListEntry>,
    onDraftDateChange: (Long) -> Unit,
    onVenueClick: () -> Unit,
    onAddPerformerClick: () -> Unit,
    onRemovePerformer: (String) -> Unit,
    onMovePerformer: (from: Int, to: Int) -> Unit,
    onAddSetListEntryClick: () -> Unit,
    onEditSetListEntryClick: (entryId: String) -> Unit,
    onEditPendingSetListEntryClick: (localId: String) -> Unit,
    onMoveSetListEntry: (from: Int, to: Int) -> Unit,
    onMovePendingSetListEntry: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DatePickerField(
                millis = draftDate,
                onMillisChange = onDraftDateChange,
                label = stringResource(R.string.performance_form_date_label),
                modifier = Modifier.weight(1f)
            )
            TimePickerField(
                millis = draftDate,
                onMillisChange = onDraftDateChange,
                label = stringResource(R.string.performance_form_time_label),
                modifier = Modifier.weight(1f)
            )
        }

        VenueField(
            venueName = draftVenueName,
            onVenueClick = onVenueClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        PerformerEditList(
            performers = draftPerformers,
            onAddPerformerClick = onAddPerformerClick,
            onRemovePerformer = onRemovePerformer,
            onMovePerformer = onMovePerformer,
            modifier = Modifier.padding(top = 16.dp)
        )

        SetListEditList(
            setList = currentSetList,
            pendingSetListEntries = pendingSetListEntries,
            onAddSetListEntryClick = onAddSetListEntryClick,
            onEditSetListEntryClick = onEditSetListEntryClick,
            onEditPendingSetListEntryClick = onEditPendingSetListEntryClick,
            onMoveSetListEntry = onMoveSetListEntry,
            onMovePendingSetListEntry = onMovePendingSetListEntry,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun PerformanceEditFormPreview() {
    ConcertTrackerTheme {
        PerformanceEditForm(
            draftDate = 1731700200000L,
            draftVenueName = "Symphony Hall",
            draftPerformers = listOf(
                Performer(id = "id1", name = "Boston Symphony Orchestra", type = PerformerType.ORCHESTRA, specialty = "Orchestra"),
                Performer(id = "id2", name = "Yo-Yo Ma", type = PerformerType.SOLO, specialty = "Cellist"),
                Performer(id = "id3", name = "Andris Nelsons", type = PerformerType.CONDUCTOR, specialty = null)
            ),
            currentSetList = emptyList(),
            pendingSetListEntries = emptyList(),
            onDraftDateChange = {},
            onVenueClick = {},
            onAddPerformerClick = {},
            onRemovePerformer = {},
            onMovePerformer = { _, _ -> },
            onAddSetListEntryClick = {},
            onEditSetListEntryClick = {},
            onEditPendingSetListEntryClick = {},
            onMoveSetListEntry = { _, _ -> },
            onMovePendingSetListEntry = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PerformanceEditFormEmptyPreview() {
    ConcertTrackerTheme {
        PerformanceEditForm(
            draftDate = null,
            draftVenueName = null,
            draftPerformers = emptyList(),
            currentSetList = emptyList(),
            pendingSetListEntries = emptyList(),
            onDraftDateChange = {},
            onVenueClick = {},
            onAddPerformerClick = {},
            onRemovePerformer = {},
            onMovePerformer = { _, _ -> },
            onAddSetListEntryClick = {},
            onEditSetListEntryClick = {},
            onEditPendingSetListEntryClick = {},
            onMoveSetListEntry = { _, _ -> },
            onMovePendingSetListEntry = { _, _ -> }
        )
    }
}
// endregion