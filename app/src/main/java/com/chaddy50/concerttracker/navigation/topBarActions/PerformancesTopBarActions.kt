package com.chaddy50.concerttracker.navigation.topBarActions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.navigation.topBarActions.syncStatusIndicator.SyncStatusIndicator

@Composable
fun PerformancesTopBarActions(onNavigateToSettings: () -> Unit) {
    SyncStatusIndicator()
    IconButton(onClick = onNavigateToSettings) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings_title)
        )
    }
}
