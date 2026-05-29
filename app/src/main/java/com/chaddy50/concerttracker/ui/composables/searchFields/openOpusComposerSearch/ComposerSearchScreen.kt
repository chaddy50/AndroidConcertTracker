package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.data.api.OpenOpusComposer

@Composable
fun ComposerSearchScreen(
    onComposerSelected: (OpenOpusComposer) -> Unit,
    onCustomComposerSelected: (composerName: String) -> Unit,
    viewModel: ComposerSearchViewModel = hiltViewModel()
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val state = viewModel.uiState
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("Composer") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            when (state) {
                is ComposerSearchUiState.Idle -> {}
                is ComposerSearchUiState.Loading -> item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ComposerSearchUiState.Empty -> item {
                    Text("No results found", modifier = Modifier.padding(16.dp))
                }
                is ComposerSearchUiState.Error -> item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ComposerSearchUiState.Results -> {
                    items(state.composers) { composer ->
                        ListItem(
                            headlineContent = { Text(composer.completeName) },
                            supportingContent = composer.epoch?.let { { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onComposerSelected(composer) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            if (state !is ComposerSearchUiState.Loading) {
                item {
                    ListItem(
                        headlineContent = { Text("Create custom composer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomDialog = true }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomComposerCreationDialog(
            initialName = viewModel.searchQuery,
            onDismiss = { showCustomDialog = false },
            onConfirm = { composerName ->
                showCustomDialog = false
                onCustomComposerSelected(composerName)
            }
        )
    }
}