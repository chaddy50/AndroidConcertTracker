package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.composables.searchFields.openOpusWorkSearch.WorkSearchScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

@Serializable
data class OpenOpusWorkSearch(
    val composerEntityId: String?,
    val composerOpenOpusId: String?,
    val composerName: String
)

data class PendingWorkResult(
    val id: String,
    val name: String,
    val composerName: String
)

fun SavedStateHandle.pendingWorkFlow(): Flow<PendingWorkResult?> = combine(
    getStateFlow<String?>("selectedWorkId", null),
    getStateFlow<String?>("selectedWorkName", null),
    getStateFlow<String?>("selectedWorkComposerName", null)
) { id, name, composerName ->
    if (id != null && name != null && composerName != null)
        PendingWorkResult(id, name, composerName)
    else null
}

fun SavedStateHandle.clearPendingWork() {
    set("selectedWorkId", null)
    set("selectedWorkName", null)
    set("selectedWorkComposerName", null)
}

fun NavGraphBuilder.openOpusWorkSearch(navController: NavController) {
    composable<OpenOpusWorkSearch> {
        WorkSearchScreen(
            onWorkSelected = { work ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedWorkId", work.id)
                    set("selectedWorkName", work.title)
                    set("selectedWorkComposerName", work.composers.firstOrNull()?.name ?: "")
                }
                navController.popBackStack()
            }
        )
    }
}
