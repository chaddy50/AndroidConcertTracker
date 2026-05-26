package com.chaddy50.concerttracker.ui.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chaddy50.concerttracker.util.epochMillisToIso
import com.chaddy50.concerttracker.util.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun mergeTimepickerWithExistingDate(hour: Int, minute: Int, existingMillis: Long?): Long {
    val existingDate = existingMillis
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        ?: LocalDate.now()
    return existingDate.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    millis: Long?,
    onMillisChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    if (isPressed) showPicker = true

    OutlinedTextField(
        value = millis?.let { formatTime(epochMillisToIso(it), context) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        interactionSource = interactionSource,
        modifier = modifier
    )

    if (showPicker) {
        val existingZoned = millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
        val pickerState = rememberTimePickerState(
            initialHour = existingZoned?.hour ?: 0,
            initialMinute = existingZoned?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onMillisChange(mergeTimepickerWithExistingDate(pickerState.hour, pickerState.minute, millis))
                    showPicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
            text = { TimePicker(state = pickerState) }
        )
    }
}