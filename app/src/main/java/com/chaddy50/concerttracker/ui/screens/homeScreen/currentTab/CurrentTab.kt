package com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chaddy50.concerttracker.ui.screens.homeScreen.composables.PerformanceCard

@Composable
fun CurrentTab(
    onPerformanceClick: (String) -> Unit,
    viewModel: CurrentTabViewModel = hiltViewModel()
) {
    when (val state = viewModel.uiState.collectAsStateWithLifecycle().value) {
        is CurrentTabUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is CurrentTabUiState.Content -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val nextUpcoming = state.nextUpcoming
                item {
                    Text(
                        text = "Next Up",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (nextUpcoming != null) {
                        PerformanceCard(
                            performance = nextUpcoming,
                            onClick = { onPerformanceClick(nextUpcoming.id) }
                        )
                    } else {
                        Text(
                            text = "Nothing coming up",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }
                if (state.recentlyAttended.isNotEmpty()) {
                    items(state.recentlyAttended, key = { it.id }) { performance ->
                        PerformanceCard(
                            performance = performance,
                            onClick = { onPerformanceClick(performance.id) }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "No recent performances",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
