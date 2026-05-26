package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
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
    composable<VenueSearch> {
        NominatimSearchScreen(
            onVenueCreated = { venue ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedVenueId", venue.id)
                    set("selectedVenueName", venue.name)
                }
                navController.popBackStack()
            }
        )
    }
}
