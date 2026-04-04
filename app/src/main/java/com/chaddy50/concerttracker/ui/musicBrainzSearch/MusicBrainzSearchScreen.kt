package com.chaddy50.concerttracker.ui.musicBrainzSearch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.entity.MusicBrainzResult

@Composable
fun MusicBrainzSearchScreen(
    onResultSelected: (MusicBrainzResult) -> Unit,
    viewModel: MusicBrainzSearchViewModel = hiltViewModel()
) {
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
        )

        when (val state = viewModel.uiState) {
            is MusicBrainzSearchUiState.Idle -> {}
            is MusicBrainzSearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MusicBrainzSearchUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.musicbrainz_search_empty_results))
                }
            }
            is MusicBrainzSearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is MusicBrainzSearchUiState.Results -> {
                MusicBrainzResultList(
                    results = state.results,
                    onResultClick = onResultSelected
                )
            }
        }
    }
}
