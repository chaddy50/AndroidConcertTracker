package com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun ServerUrlPromptDialog(
    serverUrl: String,
    onServerUrlChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.server_url_prompt_title)) },
        text = {
            Column {
                Text(stringResource(R.string.server_url_prompt_message))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChanged,
                    label = { Text(stringResource(R.string.settings_server_url_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = serverUrl.isNotBlank()) {
                Text(stringResource(R.string.server_url_prompt_confirm))
            }
        }
    )
}

// region Previews
@Preview
@Composable
fun ServerUrlPromptDialogPreview() {
    ConcertTrackerTheme {
        ServerUrlPromptDialog(
            serverUrl = "http://192.168.1.100:3000",
            onServerUrlChanged = {},
            onConfirm = {}
        )
    }
}
// endregion
