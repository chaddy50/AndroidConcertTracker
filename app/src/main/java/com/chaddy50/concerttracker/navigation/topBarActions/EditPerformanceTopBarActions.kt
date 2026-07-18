package com.chaddy50.concerttracker.navigation.topBarActions

import androidx.compose.runtime.Composable
import com.chaddy50.concerttracker.ui.composables.DeleteButton

@Composable
fun EditPerformanceTopBarActions(
    onDeletePerformance: () -> Unit
) {
    DeleteButton(
        contentDescription = "Delete performance",
        message = "Are you sure you want to delete this performance?",
        onConfirm = onDeletePerformance
    )
}
