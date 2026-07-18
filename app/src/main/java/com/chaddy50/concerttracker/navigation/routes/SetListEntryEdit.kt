package com.chaddy50.concerttracker.navigation.routes

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen.EditSetListEntryScreen
import com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen.EditSetListEntryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SetListEntryEdit(
    val performanceId: String?,
    val entryId: String?,
    val pendingLocalId: String? = null,
    val pendingWorkId: String? = null,
    val pendingWorkTitle: String? = null,
    val pendingComposerName: String? = null,
    val pendingOrder: Int? = null,
    val pendingFeaturedPerformersJson: String? = null
)

@Serializable
data class PendingSetListEntryResult(
    val pendingLocalId: String?,
    val workId: String,
    val workTitle: String,
    val composerName: String,
    val order: Int,
    val featuredPerformers: List<PendingFeaturedPerformerResult>
)

@Serializable
data class PendingFeaturedPerformerResult(
    val performerId: String,
    val name: String,
    val role: String
)

private val pendingEntryJson = Json { ignoreUnknownKeys = true }

fun SavedStateHandle.pendingSetListEntryFlow(): Flow<PendingSetListEntryResult?> =
    getStateFlow<String?>("pendingSetListEntryJson", null)
        .map { str -> str?.let { pendingEntryJson.decodeFromString<PendingSetListEntryResult>(it) } }

fun SavedStateHandle.clearPendingSetListEntry() {
    set("pendingSetListEntryJson", null)
}

fun NavGraphBuilder.setListEntryEdit(navController: NavController) {
    composable<SetListEntryEdit> { backStackEntry ->
        val viewModel: EditSetListEntryViewModel = hiltViewModel()
        val handle = backStackEntry.savedStateHandle

        val pendingWork by handle.pendingWorkFlow().collectAsStateWithLifecycle(null)
        val pendingPerformer by handle.pendingPerformerFlow().collectAsStateWithLifecycle(null)

        LaunchedEffect(pendingWork) {
            pendingWork?.let {
                viewModel.selectWork(it.id, it.name, it.composerName)
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
                // The set list is observed from Room on the Edit Performance screen, so the
                // write-through from createSetListEntry/updateSetListEntryFull re-emits automatically.
                navController.popBackStack()
            },
            onSavedAsPending = { pendingEntryData ->
                val result = PendingSetListEntryResult(
                    pendingLocalId = pendingEntryData.pendingLocalId,
                    workId = pendingEntryData.workId,
                    workTitle = pendingEntryData.workTitle,
                    composerName = pendingEntryData.composerName,
                    order = pendingEntryData.order,
                    featuredPerformers = pendingEntryData.featuredPerformers.map { p ->
                        PendingFeaturedPerformerResult(p.performerId, p.name, p.role)
                    }
                )
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "pendingSetListEntryJson",
                    pendingEntryJson.encodeToString(result)
                )
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() },
            onNavigateToSearchWork = { navController.navigate(OpenOpusComposerSearch) },
            onNavigateToSearchPerformer = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.PERFORMER)) }
        )
    }
}
