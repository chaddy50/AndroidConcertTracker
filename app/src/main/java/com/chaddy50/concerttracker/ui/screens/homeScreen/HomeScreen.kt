package com.chaddy50.concerttracker.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.navigation.routes.HomeTab
import com.chaddy50.concerttracker.navigation.routes.PastTab
import com.chaddy50.concerttracker.navigation.routes.UpcomingTab
import com.chaddy50.concerttracker.ui.screens.homeScreen.currentTab.CurrentTab
import com.chaddy50.concerttracker.ui.screens.homeScreen.pastTab.PastTab
import com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt.ServerUrlPromptDialog
import com.chaddy50.concerttracker.ui.screens.homeScreen.serverUrlPrompt.ServerUrlPromptViewModel
import com.chaddy50.concerttracker.ui.screens.homeScreen.upcomingTab.UpcomingTab

@Composable
fun HomeScreen(
    tabNavController: NavHostController,
    onPerformanceClick: (String) -> Unit,
    promptViewModel: ServerUrlPromptViewModel = hiltViewModel()
) {
    NavHost(
        navController = tabNavController,
        startDestination = HomeTab
    ) {
        composable<HomeTab> {
            CurrentTab(onPerformanceClick = onPerformanceClick)
        }
        composable<UpcomingTab> {
            UpcomingTab(onPerformanceClick = onPerformanceClick)
        }
        composable<PastTab> {
            PastTab(onPerformanceClick = onPerformanceClick)
        }
    }

    if (promptViewModel.showPrompt) {
        ServerUrlPromptDialog(
            serverUrl = promptViewModel.serverUrlInput,
            onServerUrlChanged = promptViewModel::onServerUrlInputChanged,
            onConfirm = promptViewModel::onConfirm
        )
    }
}
