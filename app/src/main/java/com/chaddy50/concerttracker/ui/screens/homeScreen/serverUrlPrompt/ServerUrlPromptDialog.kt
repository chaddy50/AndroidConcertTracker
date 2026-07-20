package com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.repository.ServerUrlValidationError
import com.chaddy50.concerttracker.ui.composables.message
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun ServerUrlPromptDialog(
    serverUrl: String,
    isValidating: Boolean,
    validationError: ServerUrlValidationError?,
    onServerUrlChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.server_url_prompt_title)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Column {
                Text(stringResource(R.string.server_url_prompt_message))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChanged,
                    label = { Text(stringResource(R.string.settings_server_url_label)) },
                    singleLine = true,
                    isError = validationError != null,
                    enabled = !isValidating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .focusRequester(focusRequester)
                )
                validationError?.let {
                    Text(
                        text = it.message(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = serverUrl.isNotBlank() && !isValidating) {
                if (isValidating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.server_url_prompt_confirm))
                }
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
            isValidating = false,
            validationError = null,
            onServerUrlChanged = {},
            onConfirm = {}
        )
    }
}

@Preview
@Composable
fun ServerUrlPromptDialogErrorPreview() {
    ConcertTrackerTheme {
        ServerUrlPromptDialog(
            serverUrl = "http://192.168.1.100:3000",
            isValidating = false,
            validationError = ServerUrlValidationError.UNREACHABLE,
            onServerUrlChanged = {},
            onConfirm = {}
        )
    }
}
// endregion
