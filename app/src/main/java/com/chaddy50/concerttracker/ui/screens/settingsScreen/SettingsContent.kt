package com.chaddy50.concerttracker.ui.screens.settingsScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.repository.ServerUrlValidationError
import com.chaddy50.concerttracker.ui.composables.message
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun SettingsContent(
    serverUrl: String,
    isValidating: Boolean,
    validationError: ServerUrlValidationError?,
    onServerUrlChanged: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChanged,
            label = { Text(stringResource(R.string.settings_server_url_label)) },
            isError = validationError != null,
            singleLine = true,
            trailingIcon = if (isValidating) {
                { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
        if (isValidating) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = stringResource(R.string.server_url_validation_checking),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        validationError?.let {
            Text(
                text = it.message(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun SettingsContentEmptyPreview() {
    ConcertTrackerTheme {
        SettingsContent(serverUrl = "", isValidating = false, validationError = null, onServerUrlChanged = {})
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentWithUrlPreview() {
    ConcertTrackerTheme {
        SettingsContent(
            serverUrl = "http://192.168.1.100:3000",
            isValidating = false,
            validationError = null,
            onServerUrlChanged = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentErrorPreview() {
    ConcertTrackerTheme {
        SettingsContent(
            serverUrl = "http://192.168.1.100:3000",
            isValidating = false,
            validationError = ServerUrlValidationError.UNREACHABLE,
            onServerUrlChanged = {}
        )
    }
}
// endregion
