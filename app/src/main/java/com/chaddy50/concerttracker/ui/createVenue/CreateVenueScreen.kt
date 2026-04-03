package com.chaddy50.concerttracker.ui.createVenue

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
import com.chaddy50.concerttracker.data.entity.Venue

@Composable
fun CreateVenueScreen(
    onVenueCreated: (Venue) -> Unit,
    viewModel: CreateVenueViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text(stringResource(R.string.create_venue_search_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        when (val state = viewModel.uiState) {
            is CreateVenueUiState.Idle -> {}
            is CreateVenueUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CreateVenueUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.create_venue_empty_results))
                }
            }
            is CreateVenueUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is CreateVenueUiState.Results -> {
                NominatimResultList(
                    results = state.results,
                    isEnabled = !viewModel.isSaving,
                    onResultClick = { result ->
                        viewModel.saveVenue(result, onVenueCreated)
                    }
                )
            }
        }
    }
}