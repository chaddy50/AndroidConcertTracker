package com.chaddy50.concerttracker.navigation.topbaractions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R

@Composable
fun PerformancesTopBarActions(onNavigateToSettings: () -> Unit) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { isMenuExpanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.top_bar_menu_content_description)
        )
    }
    DropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = { isMenuExpanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.top_bar_menu_item_settings)) },
            onClick = {
                isMenuExpanded = false
                onNavigateToSettings()
            }
        )
    }
}