package com.chaddy50.concerttracker.navigation.topBarActions

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.navigation.routes.PerformanceDetail
import com.chaddy50.concerttracker.navigation.routes.PerformanceEdit
import com.chaddy50.concerttracker.navigation.routes.Performances
import com.chaddy50.concerttracker.navigation.routes.Settings

@Composable
fun TopBarActionsRouter(
    currentDestination: NavDestination?,
    currentBackStackEntry: NavBackStackEntry?,
    navController: NavController
) {
    when {
        currentDestination?.hasRoute<Performances>() == true -> {
            PerformancesTopBarActions(
                onNavigateToSettings = { navController.navigate(Settings) }
            )
        }
        currentDestination?.hasRoute<PerformanceDetail>() == true -> {
            currentBackStackEntry?.let { entry ->
                val performanceId = entry.toRoute<PerformanceDetail>().id
                PerformanceDetailTopBarActions(
                    onNavigateToEdit = { navController.navigate(PerformanceEdit(performanceId)) }
                )
            }
        }
    }
}
