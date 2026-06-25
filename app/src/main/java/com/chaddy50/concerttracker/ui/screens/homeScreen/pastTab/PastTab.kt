package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

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
fun PastTab(
    onPerformanceClick: (String) -> Unit,
    viewModel: PastTabViewModel = hiltViewModel()
) {
    when (val state = viewModel.uiState) {
        is PastTabUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PastTabUiState.Error -> {
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
        is PastTabUiState.Success -> {
            if (state.performances.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No past concerts")
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
