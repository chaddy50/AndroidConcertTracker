package com.chaddy50.concerttracker.ui.screens.homeScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab.CurrentTab
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastTab
import com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt.ServerUrlPromptDialog
import com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt.ServerUrlPromptViewModel
import com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab.UpcomingTab
import kotlinx.coroutines.launch

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem("Home", Icons.Default.Home),
    TabItem("Upcoming", Icons.Default.DateRange),
    TabItem("Past", Icons.AutoMirrored.Default.List)
)

@Composable
fun HomeScreen(
    onTabChanged: (Int) -> Unit,
    onPerformanceClick: (String, String) -> Unit,
    onAddPerformanceClick: () -> Unit,
    promptViewModel: ServerUrlPromptViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onTabChanged(page)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPerformanceClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add performance"
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> CurrentTab(onPerformanceClick = onPerformanceClick)
                    1 -> UpcomingTab(onPerformanceClick = onPerformanceClick)
                    2 -> PastTab(onPerformanceClick = onPerformanceClick)
                }
            }
        }
    }

    if (promptViewModel.showPrompt) {
        ServerUrlPromptDialog(
            serverUrl = promptViewModel.serverUrlInput,
            isValidating = promptViewModel.isValidating,
            validationError = promptViewModel.validationError,
            onServerUrlChanged = promptViewModel::onServerUrlInputChanged,
            onConfirm = promptViewModel::onConfirm
        )
    }
}
