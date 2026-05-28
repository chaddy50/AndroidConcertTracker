package com.chaddy50.concerttracker.navigation.routes

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch.ComposerSearchScreen
import kotlinx.serialization.Serializable
import java.util.UUID

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
                    set("selectedWorkComposerId", work.composerId)
                    set("selectedWorkComposerName", work.composerName)
                }
                backStackEntry.savedStateHandle.clearPendingWork()
                navController.popBackStack()
            }
        }

        ComposerSearchScreen(
            onComposerSelected = { composer ->
                navController.navigate(OpenOpusWorkSearch(
                    composerId = composer.id,
                    composerCompleteName = composer.completeName
                ))
            },
            onCustomComposerSelected = { composerName ->
                navController.navigate(OpenOpusWorkSearch(
                    composerId = "CUSTOM-${UUID.randomUUID()}",
                    composerCompleteName = composerName
                ))
            }
        )
    }
}