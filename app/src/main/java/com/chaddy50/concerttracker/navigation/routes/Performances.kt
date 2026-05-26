package com.chaddy50.concerttracker.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.screens.performancesScreen.PerformancesScreen
import kotlinx.serialization.Serializable

@Serializable
object Performances

fun NavGraphBuilder.performances(navController: NavController) {
    composable<Performances> {
        PerformancesScreen(
            onPerformanceClick = { id -> navController.navigate(PerformanceDetail(id)) }
        )
    }
}