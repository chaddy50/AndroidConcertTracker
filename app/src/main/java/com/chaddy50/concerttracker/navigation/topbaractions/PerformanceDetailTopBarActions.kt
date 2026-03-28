package com.chaddy50.concerttracker.navigation.topbaractions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R

@Composable
fun PerformanceDetailTopBarActions(
    onNavigateToEdit: () -> Unit
) {
    IconButton(onClick = onNavigateToEdit) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.performance_detail_edit_content_description)
        )
    }
}
