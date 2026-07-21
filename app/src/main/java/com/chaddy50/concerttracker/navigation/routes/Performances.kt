package com.chaddy50.concerttracker.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
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
    onTabChanged: (Int) -> Unit
) {
    composable<Performances> {
        HomeScreen(
            onTabChanged = onTabChanged,
            onPerformanceClick = { id, date -> navController.navigate(PerformanceDetail(id, date)) },
            onAddPerformanceClick = { navController.navigate(PerformanceEdit(id = null)) }
        )
    }
}
