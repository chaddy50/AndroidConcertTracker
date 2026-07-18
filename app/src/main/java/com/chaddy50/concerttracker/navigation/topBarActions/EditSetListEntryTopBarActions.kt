package com.chaddy50.concerttracker.navigation.topBarActions

import androidx.compose.runtime.Composable
import com.chaddy50.concerttracker.ui.composables.DeleteButton

@Composable
fun EditSetListEntryTopBarActions(
    onDeleteSetListEntry: () -> Unit
) {
    DeleteButton(
        contentDescription = "Delete entry",
        message = "Are you sure you want to delete this entry?",
        onConfirm = onDeleteSetListEntry
    )
}
