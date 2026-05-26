package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch.OpenOpusWorkSearchScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

@Serializable
object OpenOpusWorkSearch

data class PendingWorkResult(
    val id: String,
    val name: String,
    val composerId: String,
    val composerName: String
)

fun SavedStateHandle.pendingWorkFlow(): Flow<PendingWorkResult?> = combine(
    getStateFlow<String?>("selectedWorkId", null),
    getStateFlow<String?>("selectedWorkName", null),
    getStateFlow<String?>("selectedWorkComposerId", null),
    getStateFlow<String?>("selectedWorkComposerName", null)
) { id, name, composerId, composerName ->
    if (id != null && name != null && composerId != null && composerName != null)
        PendingWorkResult(id, name, composerId, composerName)
    else null
}

fun SavedStateHandle.clearPendingWork() {
    set("selectedWorkId", null)
    set("selectedWorkName", null)
    set("selectedWorkComposerId", null)
    set("selectedWorkComposerName", null)
}

fun NavGraphBuilder.openOpusWorkSearch(navController: NavController) {
    composable<OpenOpusWorkSearch> {
        OpenOpusWorkSearchScreen(
            onWorkSelected = { openOpusWorkId, workTitle, openOpusComposerId, composerName ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedWorkId", openOpusWorkId)
                    set("selectedWorkName", workTitle)
                    set("selectedWorkComposerId", openOpusComposerId)
                    set("selectedWorkComposerName", composerName)
                }
                navController.popBackStack()
            }
        )
    }
}
