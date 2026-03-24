package com.chaddy50.concerttracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.chaddy50.concerttracker.navigation.topbaractions.PerformancesTopBarActions

@Composable
fun TopBarActionsRouter(
    currentDestination: NavDestination?,
    navController: NavController
) {
    when {
        currentDestination?.hasRoute<Performances>() == true -> {
            PerformancesTopBarActions(
                onNavigateToSettings = { navController.navigate(Settings) }
            )
        }
    }
}