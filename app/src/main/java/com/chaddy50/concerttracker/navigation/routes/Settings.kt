package com.chaddy50.concerttracker.navigation.routes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.screens.settingsScreen.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object Settings

fun NavGraphBuilder.settings(navController: NavHostController) {
    composable<Settings> {
        SettingsScreen(onNavigateBack = { navController.popBackStack() })
    }
}