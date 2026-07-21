package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.composables.searchFields.openOpusComposerSearch.ComposerSearchScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

@Serializable
object OpenOpusComposerSearch

data class PendingComposerResult(
    val entityId: String?,
    val openOpusId: String?,
    val name: String,
    val epoch: String?
)

fun SavedStateHandle.pendingComposerFlow(): Flow<PendingComposerResult?> = combine(
    getStateFlow<String?>("selectedComposerEntityId", null),
    getStateFlow<String?>("selectedComposerOpenOpusId", null),
    getStateFlow<String?>("selectedComposerName", null),
    getStateFlow<String?>("selectedComposerEpoch", null)
) { entityId, openOpusId, name, epoch ->
    if (name != null)
        PendingComposerResult(entityId, openOpusId, name, epoch)
    else null
}

fun SavedStateHandle.clearPendingComposer() {
    set("selectedComposerEntityId", null)
    set("selectedComposerOpenOpusId", null)
    set("selectedComposerName", null)
    set("selectedComposerEpoch", null)
}

fun NavGraphBuilder.openOpusComposerSearch(navController: NavController) {
    composable<OpenOpusComposerSearch> {
        ComposerSearchScreen(
            onComposerChosen = { composerEntityId, composerOpenOpusId, composerName, composerEpoch ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedComposerEntityId", composerEntityId)
                    set("selectedComposerOpenOpusId", composerOpenOpusId)
                    set("selectedComposerName", composerName)
                    set("selectedComposerEpoch", composerEpoch)
                }
                navController.popBackStack()
            }
        )
    }
}