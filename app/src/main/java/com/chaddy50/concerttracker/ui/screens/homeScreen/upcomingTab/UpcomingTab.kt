package com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab

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
fun UpcomingTab(
    onPerformanceClick: (String) -> Unit,
    viewModel: UpcomingTabViewModel = hiltViewModel()
) {
    when (val state = viewModel.uiState) {
        is UpcomingTabUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UpcomingTabUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = viewModel::loadData,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is UpcomingTabUiState.Success -> {
            if (state.performances.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No upcoming concerts")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(state.performances, key = { it.id }) { performance ->
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
