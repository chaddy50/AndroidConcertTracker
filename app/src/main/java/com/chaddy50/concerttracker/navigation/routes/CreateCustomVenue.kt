package com.chaddy50.concerttracker.navigation.routes

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.screens.createCustomVenueScreen.CreateCustomVenueScreen
import kotlinx.serialization.Serializable

@Serializable
object CreateCustomVenue

fun NavGraphBuilder.createCustomVenue(navController: NavController) {
    composable<CreateCustomVenue> {
        CreateCustomVenueScreen(
            onVenueCreated = { venue ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedVenueId", venue.id)
                    set("selectedVenueName", venue.name)
                }
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() }
        )
    }
}
