package com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.data.api.OpenOpusWork

@Composable
fun WorkSearchScreen(
    onWorkSelected: (OpenOpusWork) -> Unit,
    onCustomWorkSelected: (workTitle: String) -> Unit,
    viewModel: WorkSearchViewModel = hiltViewModel()
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
            label = { Text("Search works") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .focusRequester(focusRequester)
        )
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(OpenOpusGenre.entries) { genre ->
                FilterChip(
                    selected = viewModel.selectedGenre == genre,
                    onClick = { viewModel.selectGenre(genre) },
                    label = { Text(genre.displayName) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            when (state) {
                is WorkSearchUiState.Idle -> {}
                is WorkSearchUiState.Loading -> item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is WorkSearchUiState.Error -> item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is WorkSearchUiState.Results -> {
                    val filtered = viewModel.filteredWorks
                    if (filtered.isEmpty()) {
                        item {
                            Text("No works found", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(filtered) { work ->
                            ListItem(
                                headlineContent = { Text(work.title) },
                                supportingContent = work.genre?.let { { Text(it) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onWorkSelected(work) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (state !is WorkSearchUiState.Loading) {
                item {
                    ListItem(
                        headlineContent = { Text("Create custom work") },
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
        CustomWorkCreationDialog(
            composerName = viewModel.composerCompleteName,
            initialName = viewModel.searchQuery,
            onDismiss = { showCustomDialog = false },
            onConfirm = { workTitle ->
                showCustomDialog = false
                onCustomWorkSelected(workTitle)
            }
        )
    }
}