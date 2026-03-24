package com.chaddy50.concerttracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = viewModel.serverUrl,
            onValueChange = viewModel::onServerUrlChanged,
            label = { Text(stringResource(R.string.settings_server_url_label)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
