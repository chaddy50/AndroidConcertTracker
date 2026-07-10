package com.chaddy50.concerttracker.ui.composables.searchFields.nominatimSearch

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.domain.Venue

@Composable
fun NominatimSearchScreen(
    onVenueCreated: (Venue) -> Unit,
    viewModel: NominatimSearchViewModel = hiltViewModel()
) {

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                .focusRequester(focusRequester)
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
                    Text(text = state.errorType.toUserMessage(), color = MaterialTheme.colorScheme.error)
                }
            }
            is CreateVenueUiState.Results -> {
                LazyColumn {
                    items(state.rows) { row ->
                        ListItem(
                            headlineContent = { Text(row.name) },
                            supportingContent = row.address?.let { { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !viewModel.isSaving) {
                                    when (row) {
                                        is VenueSearchResult.Local ->
                                            viewModel.selectVenue(row.venue, onVenueCreated)
                                        is VenueSearchResult.FromApi ->
                                            viewModel.selectVenueFromApi(row.result, onVenueCreated)
                                    }
                                }
                        )
                        HorizontalDivider()
                    }
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
}
