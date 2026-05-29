package com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.api.MusicBrainzResult
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType

@Composable
fun MusicBrainzSearchScreen(
    onResultSelected: (MusicBrainzResult) -> Unit,
    viewModel: MusicBrainzSearchViewModel = hiltViewModel()
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
            label = { Text(stringResource(R.string.musicbrainz_search_label)) },
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
                is MusicBrainzSearchUiState.Idle -> {}
                is MusicBrainzSearchUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is MusicBrainzSearchUiState.Empty -> {
                    item {
                        Text(
                            text = stringResource(R.string.musicbrainz_search_empty_results),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is MusicBrainzSearchUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is MusicBrainzSearchUiState.Results -> {
                    items(state.results) { result ->
                        ResultItem(result = result, onClick = { onResultSelected(result) })
                    }
                }
            }

            if (state !is MusicBrainzSearchUiState.Loading) {
                val label = when (viewModel.entityType) {
                    MusicBrainzEntityType.PERFORMER -> "Create custom performer"
                    MusicBrainzEntityType.CONDUCTOR -> "Create custom conductor"
                    MusicBrainzEntityType.COMPOSER -> "Create custom composer"
                }
                item {
                    ListItem(
                        headlineContent = { Text(label) },
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
        CustomMusicBrainzEntityCreationDialog(
            entityType = viewModel.entityType,
            initialName = viewModel.searchQuery,
            onDismiss = { showCustomDialog = false },
            onConfirm = { result ->
                showCustomDialog = false
                onResultSelected(result)
            }
        )
    }
}

@Composable
private fun ResultItem(result: MusicBrainzResult, onClick: () -> Unit) {
    val supporting = when {
        result.description != null -> result.description
        result.performerType != null -> "(${result.performerType.name.lowercase().replaceFirstChar { it.uppercase() }})"
        else -> null
    }
    ListItem(
        headlineContent = { Text(result.name) },
        supportingContent = supporting?.let { { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
    HorizontalDivider()
}