package com.chaddy50.concerttracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chaddy50.concerttracker.ui.common.TopBarState
import com.chaddy50.concerttracker.ui.performances.PerformancesScreen
import com.chaddy50.concerttracker.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object Performances

@Serializable
object Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val topBarState = remember { TopBarState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarState.title) },
                actions = { topBarState.actions(this) }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Performances,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Performances> {
                PerformancesScreen(
                    onUpdateTopBar = topBarState::update,
                    onNavigateToSettings = { navController.navigate(Settings) }
                )
            }
            composable<Settings> {
                SettingsScreen(
                    onUpdateTopBar = topBarState::update
                )
            }
        }
    }
}