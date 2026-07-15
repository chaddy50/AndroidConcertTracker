package com.chaddy50.concerttracker.navigation.routes

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch.ComposerSearchScreen
import kotlinx.serialization.Serializable

@Serializable
object OpenOpusComposerSearch

fun NavGraphBuilder.openOpusComposerSearch(navController: NavController) {
    composable<OpenOpusComposerSearch> { backStackEntry ->
        val pendingWork by backStackEntry.savedStateHandle.pendingWorkFlow()
            .collectAsStateWithLifecycle(null)

        LaunchedEffect(pendingWork) {
            pendingWork?.let { work ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedWorkId", work.id)
                    set("selectedWorkName", work.name)
                    set("selectedWorkComposerName", work.composerName)
                }
                backStackEntry.savedStateHandle.clearPendingWork()
                navController.popBackStack()
            }
        }

        ComposerSearchScreen(
            onComposerChosen = { composerEntityId, composerOpenOpusId, composerName, composerEpoch ->
                navController.navigate(
                    OpenOpusWorkSearch(
                        composerEntityId = composerEntityId,
                        composerOpenOpusId = composerOpenOpusId,
                        composerName = composerName,
                        composerEpoch = composerEpoch
                    )
                )
            }
        )
    }
}