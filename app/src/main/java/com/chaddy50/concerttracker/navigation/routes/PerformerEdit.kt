package com.chaddy50.concerttracker.navigation.routes

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.screens.editPerformerScreen.EditPerformerScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PerformerEdit(val performerId: String)

@Serializable
data class PerformerUpdatedResult(
    val id: String,
    val name: String,
    val type: String,
    val specialty: String?
)

private val performerJson = Json { ignoreUnknownKeys = true }

fun SavedStateHandle.performerUpdatedFlow(): Flow<PerformerUpdatedResult?> =
    getStateFlow<String?>("performerUpdatedJson", null)
        .map { str -> str?.let { performerJson.decodeFromString<PerformerUpdatedResult>(it) } }

fun SavedStateHandle.clearPerformerUpdated() {
    set("performerUpdatedJson", null)
}

fun NavGraphBuilder.performerEdit(navController: NavController) {
    composable<PerformerEdit> {
        EditPerformerScreen(
            onSaved = { result ->
                navController.previousBackStackEntry?.savedStateHandle
                    ?.set("performerUpdatedJson", performerJson.encodeToString(result))
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() }
        )
    }
}
