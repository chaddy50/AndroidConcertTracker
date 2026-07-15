package com.chaddy50.concerttracker.navigation.routes

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.composables.searchFields.nominatimSearch.NominatimSearchScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

@Serializable
object VenueSearch

data class PendingVenueResult(val id: String, val name: String)

fun SavedStateHandle.pendingVenueFlow(): Flow<PendingVenueResult?> = combine(
    getStateFlow<String?>("selectedVenueId", null),
    getStateFlow<String?>("selectedVenueName", null)
) { id, name ->
    if (id != null && name != null) PendingVenueResult(id, name) else null
}

fun SavedStateHandle.clearPendingVenue() {
    set("selectedVenueId", null)
    set("selectedVenueName", null)
}

fun NavGraphBuilder.venueSearch(navController: NavController) {
    composable<VenueSearch> { backStackEntry ->
        // A custom venue created one screen up sets the pending-venue keys on this entry; forward
        // it to the Edit Performance entry and pop, mirroring the composer -> work -> performance flow.
        val pendingVenue by backStackEntry.savedStateHandle.pendingVenueFlow()
            .collectAsStateWithLifecycle(null)

        LaunchedEffect(pendingVenue) {
            pendingVenue?.let {
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedVenueId", it.id)
                    set("selectedVenueName", it.name)
                }
                backStackEntry.savedStateHandle.clearPendingVenue()
                navController.popBackStack()
            }
        }

        NominatimSearchScreen(
            onVenueCreated = { venue ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedVenueId", venue.id)
                    set("selectedVenueName", venue.name)
                }
                navController.popBackStack()
            },
            onCreateCustomClick = { navController.navigate(CreateCustomVenue) }
        )
    }
}
