package com.chaddy50.concerttracker.ui.screens.homeScreen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.screens.homeScreen.composables.BottomNavigationBar
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
    onPerformanceClick: (String, String) -> Unit,
    onAddPerformanceClick: () -> Unit,
    promptViewModel: ServerUrlPromptViewModel = hiltViewModel()
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottomNavigationBar(tabNavController = tabNavController)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPerformanceClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add performance"
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = HomeTab,
            modifier = Modifier.padding(innerPadding)
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
