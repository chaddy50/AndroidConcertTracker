package com.chaddy50.concerttracker.ui.performanceEdit.fields

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R

@Composable
fun VenueField(
    venueName: String?,
    onVenueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    if (isPressed) onVenueClick()

    OutlinedTextField(
        value = venueName ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.performance_form_venue_label)) },
        interactionSource = interactionSource,
        modifier = modifier
    )
}