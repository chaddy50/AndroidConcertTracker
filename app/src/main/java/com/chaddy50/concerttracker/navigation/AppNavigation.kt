package com.chaddy50.concerttracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.enum.MusicBrainzEntityType
import com.chaddy50.concerttracker.ui.createVenue.CreateVenueScreen
import com.chaddy50.concerttracker.ui.musicBrainzSearch.MusicBrainzSearchScreen
import com.chaddy50.concerttracker.ui.performanceDetail.PerformanceDetailScreen
import com.chaddy50.concerttracker.ui.performanceEdit.PerformanceEditScreen
import com.chaddy50.concerttracker.ui.performances.PerformancesScreen
import com.chaddy50.concerttracker.ui.openOpusWorkSearch.OpenOpusWorkSearchScreen
import com.chaddy50.concerttracker.ui.setListEntryEdit.SetListEntryEditScreen
import com.chaddy50.concerttracker.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val canNavigateBack = currentBackStackEntry != null && navController.previousBackStackEntry != null

    val title = when {
        currentDestination?.hasRoute<Performances>() == true -> stringResource(R.string.performances_title)
        currentDestination?.hasRoute<Settings>() == true -> stringResource(R.string.settings_title)
        currentDestination?.hasRoute<PerformanceDetail>() == true -> stringResource(R.string.performance_detail_title)
        currentDestination?.hasRoute<PerformanceEdit>() == true -> {
            val isNew = currentBackStackEntry?.toRoute<PerformanceEdit>()?.id == null
            if (isNew) stringResource(R.string.performance_form_new_title)
            else stringResource(R.string.performance_form_edit_title)
        }
        currentDestination?.hasRoute<CreateVenue>() == true -> stringResource(R.string.create_venue_title)
        currentDestination?.hasRoute<MusicBrainzSearch>() == true -> stringResource(R.string.musicbrainz_search_title)
        currentDestination?.hasRoute<OpenOpusWorkSearch>() == true -> stringResource(R.string.open_opus_work_search_title)
        currentDestination?.hasRoute<SetListEntryEdit>() == true -> {
            val isNew = currentBackStackEntry?.toRoute<SetListEntryEdit>()?.entryId == null
            if (isNew) stringResource(R.string.set_list_entry_form_add_title)
            else stringResource(R.string.set_list_entry_form_edit_title)
        }
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back_content_description)
                            )
                        }
                    }
                },
                actions = {
                    TopBarActionsRouter(
                        currentDestination = currentDestination,
                        currentBackStackEntry = currentBackStackEntry,
                        navController = navController
                    )
                }
            )
        },
        floatingActionButton = {
            if (currentDestination?.hasRoute<Performances>() == true) {
                FloatingActionButton(
                    onClick = { navController.navigate(PerformanceEdit(id = null)) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.performances_add_content_description)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Performances,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Performances> {
                PerformancesScreen(
                    onPerformanceClick = { id ->
                        navController.navigate(PerformanceDetail(id))
                    }
                )
            }
            composable<Settings> {
                SettingsScreen()
            }
            composable<PerformanceDetail> {
                PerformanceDetailScreen()
            }
            composable<PerformanceEdit> { backStackEntry ->
                // NavBackStackEntry.savedStateHandle is the correct place to observe results
                // returned by child destinations — it is separate from the ViewModel's SavedStateHandle.
                val shouldReload by backStackEntry.savedStateHandle
                    .getStateFlow<Boolean>("shouldReload", false)
                    .collectAsStateWithLifecycle()
                val pendingVenueId by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedVenueId", null)
                    .collectAsStateWithLifecycle()
                val pendingVenueName by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedVenueName", null)
                    .collectAsStateWithLifecycle()
                val pendingConductorId by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedConductorId", null)
                    .collectAsStateWithLifecycle()
                val pendingConductorName by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedConductorName", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerId by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerId", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerName by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerName", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerType by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerType", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerSpecialty by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerSpecialty", null)
                    .collectAsStateWithLifecycle()
                val performanceId = backStackEntry.toRoute<PerformanceEdit>().id

                PerformanceEditScreen(
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    onNavigateToCreateVenue = { navController.navigate(CreateVenue) },
                    onNavigateToSearchPerformer = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.PERFORMER)) },
                    onNavigateToSearchConductor = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.CONDUCTOR)) },
                    shouldReload = shouldReload,
                    onReloaded = { backStackEntry.savedStateHandle["shouldReload"] = false },
                    onNavigateToAddSetListEntry = {
                        navController.navigate(SetListEntryEdit(performanceId = performanceId!!, entryId = null))
                    },
                    onNavigateToEditSetListEntry = { entryId ->
                        navController.navigate(SetListEntryEdit(performanceId = performanceId!!, entryId = entryId))
                    },
                    pendingVenueId = pendingVenueId,
                    pendingVenueName = pendingVenueName,
                    pendingConductorId = pendingConductorId,
                    pendingConductorName = pendingConductorName,
                    pendingPerformerId = pendingPerformerId,
                    pendingPerformerName = pendingPerformerName,
                    pendingPerformerType = pendingPerformerType,
                    pendingPerformerSpecialty = pendingPerformerSpecialty
                )
            }
            composable<CreateVenue> {
                CreateVenueScreen(
                    onVenueCreated = { venue ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("selectedVenueId", venue.id)
                            set("selectedVenueName", venue.name)
                        }
                        navController.popBackStack()
                    }
                )
            }
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
            composable<SetListEntryEdit> { backStackEntry ->
                val pendingWorkId by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedWorkId", null)
                    .collectAsStateWithLifecycle()
                val pendingWorkName by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedWorkName", null)
                    .collectAsStateWithLifecycle()
                val pendingWorkComposerId by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedWorkComposerId", null)
                    .collectAsStateWithLifecycle()
                val pendingWorkComposerName by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedWorkComposerName", null)
                    .collectAsStateWithLifecycle()
                val pendingConductorId by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedConductorId", null)
                    .collectAsStateWithLifecycle()
                val pendingConductorName by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedConductorName", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerId by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerId", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerName by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerName", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerType by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerType", null)
                    .collectAsStateWithLifecycle()
                val pendingPerformerSpecialty by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("selectedPerformerSpecialty", null)
                    .collectAsStateWithLifecycle()

                SetListEntryEditScreen(
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
                    onNavigateToSearchConductor = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.CONDUCTOR)) },
                    onNavigateToSearchPerformer = { navController.navigate(MusicBrainzSearch(MusicBrainzEntityType.PERFORMER)) },
                    pendingWorkId = pendingWorkId,
                    pendingWorkName = pendingWorkName,
                    pendingWorkComposerId = pendingWorkComposerId,
                    pendingWorkComposerName = pendingWorkComposerName,
                    pendingConductorId = pendingConductorId,
                    pendingConductorName = pendingConductorName,
                    pendingPerformerId = pendingPerformerId,
                    pendingPerformerName = pendingPerformerName,
                    pendingPerformerType = pendingPerformerType,
                    pendingPerformerSpecialty = pendingPerformerSpecialty
                )
            }
        }
    }
}
