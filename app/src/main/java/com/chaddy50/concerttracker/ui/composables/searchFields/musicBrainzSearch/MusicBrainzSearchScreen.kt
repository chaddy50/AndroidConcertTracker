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
import com.chaddy50.concerttracker.data.domain.Performer
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.data.enum.PerformerType

@Composable
fun MusicBrainzSearchScreen(
    onPerformerSelected: (Performer) -> Unit,
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
                            Text(text = state.errorType.toUserMessage(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is MusicBrainzSearchUiState.Results -> {
                    items(state.rows) { row ->
                        ResultItem(
                            name = row.name,
                            supporting = row.type,
                            enabled = !viewModel.isSaving,
                            onClick = {
                                when (row) {
                                    is PerformerSearchResult.Local ->
                                        viewModel.selectPerformer(row.performer, onPerformerSelected)
                                    is PerformerSearchResult.FromApi ->
                                        viewModel.selectPerformerFromApi(row.result, onPerformerSelected)
                                }
                            }
                        )
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
        if (viewModel.saveError != null) {
            Text(
                text = viewModel.saveError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showCustomDialog) {
        CustomMusicBrainzEntityCreationDialog(
            entityType = viewModel.entityType,
            initialName = viewModel.searchQuery,
            onDismiss = { showCustomDialog = false },
            onConfirm = { result ->
                showCustomDialog = false
                viewModel.createCustomPerformer(
                    name = result.name,
                    type = result.performerType ?: PerformerType.OTHER,
                    specialty = result.description,
                    onSelected = onPerformerSelected
                )
            }
        )
    }
}

@Composable
private fun ResultItem(name: String, supporting: String?, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = supporting?.let { { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    )
    HorizontalDivider()
}