package com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.chaddy50.concerttracker.ui.screens.homeScreen.composables.PerformanceCard
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.composables.YearHeader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PastTab(
    onPerformanceClick: (String, String) -> Unit,
    viewModel: PastTabViewModel = hiltViewModel()
) {
    val items = viewModel.pagedItems.collectAsLazyPagingItems()
    val refresh = items.loadState.refresh

    when {
        refresh is LoadState.Loading && items.itemCount == 0 -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        refresh is LoadState.NotLoading && items.itemCount == 0 -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No past concerts")
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                for (index in 0 until items.itemCount) {
                    when (val item = items.peek(index)) {
                        is PastListItem.Header -> {
                            stickyHeader(key = "header-$index", contentType = "header") {
                                YearHeader(item.yearLabel)
                            }
                        }
                        is PastListItem.Entry -> {
                            item(key = item.performance.id, contentType = "entry") {
                                items[index]
                                PerformanceCard(
                                    performance = item.performance,
                                    onClick = { onPerformanceClick(item.performance.id, item.performance.date) }
                                )
                            }
                        }
                        null -> item {}
                    }
                }
            }
        }
    }
}
