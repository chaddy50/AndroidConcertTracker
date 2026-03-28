package com.chaddy50.concerttracker.ui.performances

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun PerformancesScreen(
    onPerformanceClick: (String) -> Unit,
    viewModel: PerformancesViewModel = hiltViewModel()
) {
    PerformancesContent(
        uiState = viewModel.uiState,
        onPerformanceClick = onPerformanceClick,
        onRetry = viewModel::loadPerformances
    )
}

@Composable
fun PerformancesContent(
    uiState: PerformancesUiState,
    onPerformanceClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (val state = uiState) {
        is PerformancesUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PerformancesUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is PerformancesUiState.Success -> {
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

// region Previews
private val previewPerformance = Performance(
    id = "1",
    date = "2024-11-15T19:30:00.000Z",
    venue = Venue(id = "1", name = "Royal Albert Hall", osmId = "123456"),
    conductor = Performer(id = "1", name = "Simon Rattle", type = PerformerType.CONDUCTOR),
    status = PerformanceStatus.ATTENDED
)

@Preview(showBackground = true)
@Composable
fun PerformancesContentLoadingPreview() {
    ConcertTrackerTheme {
        PerformancesContent(
            uiState = PerformancesUiState.Loading,
            onPerformanceClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PerformancesContentErrorPreview() {
    ConcertTrackerTheme {
        PerformancesContent(
            uiState = PerformancesUiState.Error("Could not connect to server"),
            onPerformanceClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PerformancesContentSuccessPreview() {
    ConcertTrackerTheme {
        PerformancesContent(
            uiState = PerformancesUiState.Success(listOf(previewPerformance)),
            onPerformanceClick = {},
            onRetry = {}
        )
    }
}
// endregion