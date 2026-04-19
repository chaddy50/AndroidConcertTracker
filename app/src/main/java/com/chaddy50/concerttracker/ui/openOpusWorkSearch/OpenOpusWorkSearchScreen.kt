package com.chaddy50.concerttracker.ui.openOpusWorkSearch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.data.api.OpenOpusComposer
import com.chaddy50.concerttracker.data.api.OpenOpusWork

@Composable
fun OpenOpusWorkSearchScreen(
    onWorkSelected: (openOpusWorkId: String, workTitle: String, openOpusComposerId: String, composerName: String) -> Unit,
    viewModel: OpenOpusWorkSearchViewModel = hiltViewModel()
) {
    when (val state = viewModel.uiState) {
        is OpenOpusWorkSearchUiState.ComposerSearch -> {
            ComposerSearchContent(
                query = viewModel.composerSearchQuery,
                isLoading = state.isLoading,
                composers = state.composers,
                error = state.error,
                onQueryChange = viewModel::updateComposerSearchQuery,
                onSearch = viewModel::searchComposers,
                onComposerClick = viewModel::selectComposer
            )
        }
        is OpenOpusWorkSearchUiState.WorkList -> {
            BackHandler { viewModel.backToComposerSearch() }
            WorkListContent(
                state = state,
                workSearchQuery = viewModel.workSearchQuery,
                filteredWorks = viewModel.filteredWorks,
                onWorkSearchQueryChange = viewModel::updateWorkSearchQuery,
                onGenreSelect = viewModel::selectGenre,
                onChangeComposer = viewModel::backToComposerSearch,
                onWorkClick = { work ->
                    onWorkSelected(
                        work.id,
                        work.title,
                        state.composer.id,
                        state.composer.completeName
                    )
                }
            )
        }
    }
}

@Composable
private fun ComposerSearchContent(
    query: String,
    isLoading: Boolean,
    composers: List<OpenOpusComposer>,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onComposerClick: (OpenOpusComposer) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Composer") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
            composers.isEmpty() && query.isNotBlank() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No composers found")
            }
            else -> LazyColumn {
                items(composers) { composer ->
                    ListItem(
                        headlineContent = { Text(composer.completeName) },
                        supportingContent = composer.epoch?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onComposerClick(composer) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun WorkListContent(
    state: OpenOpusWorkSearchUiState.WorkList,
    workSearchQuery: String,
    filteredWorks: List<OpenOpusWork>,
    onWorkSearchQueryChange: (String) -> Unit,
    onGenreSelect: (OpenOpusGenre) -> Unit,
    onChangeComposer: () -> Unit,
    onWorkClick: (OpenOpusWork) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(
            onClick = onChangeComposer,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        ) {
            Text("← ${state.composer.completeName}")
        }
        OutlinedTextField(
            value = workSearchQuery,
            onValueChange = onWorkSearchQueryChange,
            label = { Text("Search works") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(OpenOpusGenre.entries) { genre ->
                FilterChip(
                    selected = state.selectedGenre == genre,
                    onClick = { onGenreSelect(genre) },
                    label = { Text(genre.displayName) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = state.error, color = MaterialTheme.colorScheme.error)
            }
            filteredWorks.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No works found")
            }
            else -> LazyColumn {
                items(filteredWorks) { work ->
                    ListItem(
                        headlineContent = { Text(work.title) },
                        supportingContent = work.genre?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorkClick(work) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
