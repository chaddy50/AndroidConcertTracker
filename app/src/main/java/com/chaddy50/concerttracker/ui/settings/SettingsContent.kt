package com.chaddy50.concerttracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun SettingsContent(serverUrl: String, onServerUrlChanged: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChanged,
            label = { Text(stringResource(R.string.settings_server_url_label)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun SettingsContentEmptyPreview() {
    ConcertTrackerTheme {
        SettingsContent(serverUrl = "", onServerUrlChanged = {})
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentWithUrlPreview() {
    ConcertTrackerTheme {
        SettingsContent(serverUrl = "http://192.168.1.100:3000", onServerUrlChanged = {})
    }
}
// endregion