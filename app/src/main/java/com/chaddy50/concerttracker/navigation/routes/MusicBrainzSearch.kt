package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.composables.searchFields.musicBrainzSearch.MusicBrainzSearchScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

@Serializable
data object MusicBrainzSearch

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
    composable<MusicBrainzSearch> {
        MusicBrainzSearchScreen(
            onPerformerSelected = { performer ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("selectedPerformerId", performer.id)
                    set("selectedPerformerName", performer.name)
                    set("selectedPerformerType", performer.type.name)
                    set("selectedPerformerSpecialty", performer.specialty)
                }
                navController.popBackStack()
            }
        )
    }
}
