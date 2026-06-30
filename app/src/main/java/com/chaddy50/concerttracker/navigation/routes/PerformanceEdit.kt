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
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.PendingFeaturedPerformer
import com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen.DraftFeaturedPerformer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PerformanceEdit(val id: String?)

fun NavGraphBuilder.performanceEdit(navController: NavController) {
    composable<PerformanceEdit> { backStackEntry ->
        val viewModel: EditPerformanceViewModel = hiltViewModel()
        val performanceId = backStackEntry.toRoute<PerformanceEdit>().id
        val handle = backStackEntry.savedStateHandle

        val pendingVenue by handle.pendingVenueFlow().collectAsStateWithLifecycle(null)
        val pendingPerformer by handle.pendingPerformerFlow().collectAsStateWithLifecycle(null)
        val pendingSetListEntry by handle.pendingSetListEntryFlow().collectAsStateWithLifecycle(null)

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

        LaunchedEffect(pendingSetListEntry) {
            pendingSetListEntry?.let { result ->
                val performers = result.featuredPerformers.map { p ->
                    PendingFeaturedPerformer(p.performerId, p.name, p.role)
                }
                if (result.pendingLocalId != null) {
                    viewModel.replacePendingSetListEntry(result.pendingLocalId, result.workId, result.workTitle, result.composerName, result.order, performers)
                } else {
                    viewModel.addPendingSetListEntry(result.workId, result.workTitle, result.composerName, result.order, performers)
                }
                handle.clearPendingSetListEntry()
            }
        }

        EditPerformanceScreen(
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() },
            onNavigateToCreateVenue = { navController.navigate(VenueSearch) },
            onNavigateToSearchPerformer = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.PERFORMER)) },
            onNavigateToAddSetListEntry = {
                navController.navigate(SetListEntryEdit(performanceId = performanceId, entryId = null))
            },
            onNavigateToEditSetListEntry = { entryId ->
                navController.navigate(SetListEntryEdit(performanceId = performanceId, entryId = entryId))
            },
            onNavigateToEditPendingSetListEntry = { localId ->
                val entry = viewModel.pendingSetListEntries.find { it.localId == localId } ?: return@EditPerformanceScreen
                navController.navigate(
                    SetListEntryEdit(
                        performanceId = null,
                        entryId = null,
                        pendingLocalId = entry.localId,
                        pendingWorkId = entry.workId,
                        pendingWorkTitle = entry.workTitle,
                        pendingComposerName = entry.composerName,
                        pendingOrder = entry.order,
                        pendingFeaturedPerformersJson = Json.encodeToString(
                            entry.featuredPerformers.map { DraftFeaturedPerformer(it.performerId, it.name, it.role) }
                        )
                    )
                )
            }
        )
    }
}