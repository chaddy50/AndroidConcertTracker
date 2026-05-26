package com.chaddy50.concerttracker.navigation.routes

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen.EditSetListEntryScreen
import com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen.EditSetListEntryViewModel
import kotlinx.serialization.Serializable

@Serializable
data class SetListEntryEdit(val performanceId: String, val entryId: String?)

fun NavGraphBuilder.setListEntryEdit(navController: NavController) {
    composable<SetListEntryEdit> { backStackEntry ->
        val viewModel: EditSetListEntryViewModel = hiltViewModel()
        val handle = backStackEntry.savedStateHandle

        val pendingWork by handle.pendingWorkFlow().collectAsStateWithLifecycle(null)
        val pendingPerformer by handle.pendingPerformerFlow().collectAsStateWithLifecycle(null)

        LaunchedEffect(pendingWork) {
            pendingWork?.let {
                viewModel.selectWork(it.id, it.name, it.composerId, it.composerName)
                handle.clearPendingWork()
            }
        }

        LaunchedEffect(pendingPerformer) {
            pendingPerformer?.let {
                viewModel.addDraftFeaturedPerformer(it.id, it.name, it.type, it.specialty)
                handle.clearPendingPerformer()
            }
        }

        EditSetListEntryScreen(
            onSaved = {
                navController.previousBackStackEntry?.savedStateHandle?.set("shouldReload", true)
                navController.popBackStack()
            },
            onDeleted = {
                navController.previousBackStackEntry?.savedStateHandle?.set("shouldReload", true)
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() },
            onNavigateToSearchWork = { navController.navigate(OpenOpusWorkSearch) },
            onNavigateToSearchPerformer = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.PERFORMER)) }
        )
    }
}