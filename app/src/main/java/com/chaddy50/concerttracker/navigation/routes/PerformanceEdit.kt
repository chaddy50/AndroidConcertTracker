package com.chaddy50.concerttracker.navigation.routes

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.EditPerformanceScreen
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.EditPerformanceViewModel
import kotlinx.serialization.Serializable

@Serializable
data class PerformanceEdit(val id: String?)

fun NavGraphBuilder.performanceEdit(navController: NavController) {
    composable<PerformanceEdit> { backStackEntry ->
        val viewModel: EditPerformanceViewModel = hiltViewModel()
        val performanceId = backStackEntry.toRoute<PerformanceEdit>().id
        val handle = backStackEntry.savedStateHandle

        val pendingVenue by handle.pendingVenueFlow().collectAsStateWithLifecycle(null)
        val pendingPerformer by handle.pendingPerformerFlow().collectAsStateWithLifecycle(null)
        val shouldReload by handle.getStateFlow<Boolean>("shouldReload", false).collectAsStateWithLifecycle()

        LaunchedEffect(pendingVenue) {
            pendingVenue?.let {
                viewModel.updateDraftVenue(it.id, it.name)
                handle.clearPendingVenue()
            }
        }

        LaunchedEffect(pendingPerformer) {
            pendingPerformer?.let {
                viewModel.addDraftPerformer(it.id, it.name, it.type, it.specialty)
                handle.clearPendingPerformer()
            }
        }

        LaunchedEffect(shouldReload) {
            if (shouldReload) {
                viewModel.refreshSetList()
                handle["shouldReload"] = false
            }
        }

        EditPerformanceScreen(
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() },
            onNavigateToCreateVenue = { navController.navigate(VenueSearch) },
            onNavigateToSearchPerformer = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.PERFORMER)) },
            onNavigateToAddSetListEntry = {
                navController.navigate(SetListEntryEdit(performanceId = performanceId!!, entryId = null))
            },
            onNavigateToEditSetListEntry = { entryId ->
                navController.navigate(SetListEntryEdit(performanceId = performanceId!!, entryId = entryId))
            }
        )
    }
}