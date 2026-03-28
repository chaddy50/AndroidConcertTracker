package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
fun PerformanceDetailScreen(viewModel: PerformanceDetailViewModel = hiltViewModel()) {
    PerformanceDetailContent(
        uiState = viewModel.uiState,
        onRetry = viewModel::loadPerformance
    )
}

@Composable
fun PerformanceDetailContent(
    uiState: PerformanceDetailUiState,
    onRetry: () -> Unit
) {
    when (val state = uiState) {
        is PerformanceDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PerformanceDetailUiState.Error -> {
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
        is PerformanceDetailUiState.Success -> {
            PerformanceDetail(performance = state.performance)
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
fun PerformanceDetailContentLoadingPreview() {
    ConcertTrackerTheme {
        PerformanceDetailContent(
            uiState = PerformanceDetailUiState.Loading,
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PerformanceDetailContentErrorPreview() {
    ConcertTrackerTheme {
        PerformanceDetailContent(
            uiState = PerformanceDetailUiState.Error("Could not load performance"),
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PerformanceDetailContentSuccessPreview() {
    ConcertTrackerTheme {
        PerformanceDetailContent(
            uiState = PerformanceDetailUiState.Success(previewPerformance),
            onRetry = {}
        )
    }
}
// endregion