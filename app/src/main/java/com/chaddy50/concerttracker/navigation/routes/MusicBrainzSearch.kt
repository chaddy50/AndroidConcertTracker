package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch.MusicBrainzSearchScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

@Serializable
data class MusicBrainzSearch(val entityType: MusicBrainzEntityType)

data class PendingPerformerResult(
    val id: String,
    val name: String,
    val type: String?,
    val specialty: String?
)

fun SavedStateHandle.pendingPerformerFlow(): Flow<PendingPerformerResult?> = combine(
    getStateFlow<String?>("selectedPerformerId", null),
    getStateFlow<String?>("selectedPerformerName", null),
    getStateFlow<String?>("selectedPerformerType", null),
    getStateFlow<String?>("selectedPerformerSpecialty", null)
) { id, name, type, specialty ->
    if (id != null && name != null) PendingPerformerResult(id, name, type, specialty) else null
}

fun SavedStateHandle.clearPendingPerformer() {
    set("selectedPerformerId", null)
    set("selectedPerformerName", null)
    set("selectedPerformerType", null)
    set("selectedPerformerSpecialty", null)
}

fun NavGraphBuilder.musicBrainzSearch(navController: NavController) {
    composable<MusicBrainzSearch> { backStackEntry ->
        val entityType = backStackEntry.toRoute<MusicBrainzSearch>().entityType
        MusicBrainzSearchScreen(
            onResultSelected = { result ->
                val (idKey, nameKey) = when (entityType) {
                    MusicBrainzEntityType.PERFORMER -> "selectedPerformerId" to "selectedPerformerName"
                    MusicBrainzEntityType.CONDUCTOR -> "selectedConductorId" to "selectedConductorName"
                    MusicBrainzEntityType.COMPOSER -> "selectedComposerId" to "selectedComposerName"
                }
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set(idKey, result.id)
                    set(nameKey, result.name)
                    if (entityType == MusicBrainzEntityType.PERFORMER) {
                        set("selectedPerformerType", result.performerType?.name)
                        set("selectedPerformerSpecialty", result.description)
                    }
                }
                navController.popBackStack()
            }
        )
    }
}
