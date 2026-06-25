package com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.ui.screens.performancesScreen.PerformanceCard

@Composable
fun CurrentTab(
    onPerformanceClick: (String) -> Unit,
    viewModel: CurrentTabViewModel = hiltViewModel()
) {
    when (val state = viewModel.uiState) {
        is CurrentTabUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is CurrentTabUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.errorType.toUserMessage(), color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = viewModel::loadData,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is CurrentTabUiState.Success -> {
            if (state.nextUpcoming == null && state.recentAttended.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No performances")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (state.nextUpcoming != null) {
                        item {
                            Text(
                                text = "Next Up",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            PerformanceCard(
                                performance = state.nextUpcoming,
                                onClick = { onPerformanceClick(state.nextUpcoming.id) }
                            )
                        }
                    }

                    if (state.recentAttended.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(top = if (state.nextUpcoming != null) 16.dp else 0.dp)
                                    .padding(bottom = 8.dp)
                            )
                        }
                        items(state.recentAttended, key = { it.id }) { performance ->
                            PerformanceCard(
                                performance = performance,
                                onClick = { onPerformanceClick(performance.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
