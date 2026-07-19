package com.chaddy50.concerttracker.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.screens.homeScreen.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object Performances

@Serializable
object HomeTab

@Serializable
object UpcomingTab

@Serializable
object PastTab

fun NavGraphBuilder.performances(
    navController: NavController,
    tabNavController: NavHostController
) {
    composable<Performances> {
        HomeScreen(
            tabNavController = tabNavController,
            onPerformanceClick = { id -> navController.navigate(PerformanceDetail(id)) },
            onAddPerformanceClick = { navController.navigate(PerformanceEdit(id = null)) }
        )
    }
}
