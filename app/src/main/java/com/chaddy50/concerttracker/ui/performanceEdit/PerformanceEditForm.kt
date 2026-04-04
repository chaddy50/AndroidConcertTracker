package com.chaddy50.concerttracker.ui.performanceEdit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import com.chaddy50.concerttracker.util.epochMillisToIso
import com.chaddy50.concerttracker.util.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceEditForm(
    draftDate: Long?,
    draftVenueName: String?,
    draftConductorName: String?,
    draftPerformers: List<Pair<String, String>>,
    draftStatus: PerformanceStatus?,
    onDraftDateChange: (Long) -> Unit,
    onDraftStatusChange: (PerformanceStatus) -> Unit,
    onVenueClick: () -> Unit,
    onConductorClick: () -> Unit,
    onAddPerformerClick: () -> Unit,
    onRemovePerformer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val dateInteractionSource = remember { MutableInteractionSource() }
        val isDatePressed by dateInteractionSource.collectIsPressedAsState()
        if (isDatePressed) showDatePicker = true

        OutlinedTextField(
            value = draftDate?.let { formatDate(epochMillisToIso(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.performance_form_date_label)) },
            interactionSource = dateInteractionSource,
            modifier = Modifier.fillMaxWidth()
        )
        val venueInteractionSource = remember { MutableInteractionSource() }
        val isVenuePressed by venueInteractionSource.collectIsPressedAsState()
        if (isVenuePressed) onVenueClick()

        OutlinedTextField(
            value = draftVenueName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.performance_form_venue_label)) },
            interactionSource = venueInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        val conductorInteractionSource = remember { MutableInteractionSource() }
        val isConductorPressed by conductorInteractionSource.collectIsPressedAsState()
        if (isConductorPressed) onConductorClick()

        OutlinedTextField(
            value = draftConductorName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.performance_form_conductor_label)) },
            interactionSource = conductorInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Text(
            text = stringResource(R.string.performance_form_performers_label),
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        draftPerformers.forEach { (performerId, performerName) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = performerName, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemovePerformer(performerId) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                }
            }
        }
        TextButton(
            onClick = onAddPerformerClick,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(stringResource(R.string.performance_form_add_performer))
        }

        StatusDropdown(
            selectedStatus = draftStatus ?: PerformanceStatus.UPCOMING,
            onStatusSelected = onDraftStatusChange,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = draftDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDraftDateChange(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
            draftConductorName = "Andris Nelsons",
            draftPerformers = listOf("id1" to "Boston Symphony Orchestra"),
            draftStatus = PerformanceStatus.ATTENDED,
            onDraftDateChange = {},
            onDraftStatusChange = {},
            onVenueClick = {},
            onConductorClick = {},
            onAddPerformerClick = {},
            onRemovePerformer = {}
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
            draftConductorName = null,
            draftPerformers = emptyList(),
            draftStatus = PerformanceStatus.UPCOMING,
            onDraftDateChange = {},
            onDraftStatusChange = {},
            onVenueClick = {},
            onConductorClick = {},
            onAddPerformerClick = {},
            onRemovePerformer = {}
        )
    }
}
// endregion