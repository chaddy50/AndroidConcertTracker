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
import com.chaddy50.concerttracker.data.domain.Work

@Composable
fun WorkSearchScreen(
    onWorkSelected: (Work) -> Unit,
    viewModel: WorkSearchViewModel = hiltViewModel()
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val state = viewModel.uiState
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = viewModel.composerName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
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
                is WorkSearchUiState.Empty -> item {
                    Text("No works found", modifier = Modifier.padding(16.dp))
                }
                is WorkSearchUiState.Error -> item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.errorType.toUserMessage(), color = MaterialTheme.colorScheme.error)
                    }
                }
                is WorkSearchUiState.Results -> {
                    items(state.rows) { row ->
                        ListItem(
                            headlineContent = { Text(row.title) },
                            supportingContent = row.genre?.let { { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !viewModel.isSaving) {
                                    when (row) {
                                        is WorkSearchResult.Local ->
                                            viewModel.selectWork(row.work, onWorkSelected)
                                        is WorkSearchResult.FromApi ->
                                            viewModel.selectWorkFromApi(row.work, onWorkSelected)
                                    }
                                }
                        )
                        HorizontalDivider()
                    }
                }
            }

            if (state !is WorkSearchUiState.Loading) {
                item {
                    ListItem(
                        headlineContent = { Text("Create custom work") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !viewModel.isSaving) { showCustomDialog = true }
                    )
                    HorizontalDivider()
                }
            }
        }
        if (viewModel.saveError != null) {
            Text(
                text = viewModel.saveError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showCustomDialog) {
        CustomWorkCreationDialog(
            composerName = viewModel.composerName,
            initialName = viewModel.searchQuery,
            onDismiss = { showCustomDialog = false },
            onConfirm = { workTitle ->
                showCustomDialog = false
                viewModel.createCustomWork(workTitle, onWorkSelected)
            }
        )
    }
}