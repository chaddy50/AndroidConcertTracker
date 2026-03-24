package com.chaddy50.concerttracker.ui.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R

@Composable
fun SettingsScreen(
    onUpdateTopBar: (String, @Composable RowScope.() -> Unit) -> Unit
) {
    val title = stringResource(R.string.settings_title)

    LaunchedEffect(Unit) {
        onUpdateTopBar(title) {}
    }

    Text("Settings")
}