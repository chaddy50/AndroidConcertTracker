package com.chaddy50.concerttracker.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.ui.platform.LocalContext
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.navigation.routes.*
import com.chaddy50.concerttracker.navigation.topBarActions.TopBarActionsRouter
import com.chaddy50.concerttracker.util.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationHost() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val canNavigateBack = currentBackStackEntry != null && navController.previousBackStackEntry != null
    var currentTabIndex by remember { mutableIntStateOf(0) }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val title = when {
        currentDestination?.hasRoute<Performances>() == true -> when (currentTabIndex) {
            0 -> "Home"
            1 -> "Upcoming"
            2 -> "Past"
            else -> ""
        }
        currentDestination?.hasRoute<Settings>() == true -> stringResource(R.string.settings_title)
        currentDestination?.hasRoute<PerformanceDetail>() == true -> {
            val date = currentBackStackEntry?.toRoute<PerformanceDetail>()?.date ?: ""
            if (date.isNotEmpty()) formatDate(date, LocalContext.current) else ""
        }
        currentDestination?.hasRoute<PerformanceEdit>() == true -> {
            val isNew = currentBackStackEntry?.toRoute<PerformanceEdit>()?.id == null
            if (isNew) stringResource(R.string.performance_form_new_title)
            else stringResource(R.string.performance_form_edit_title)
        }
        currentDestination?.hasRoute<VenueSearch>() == true -> stringResource(R.string.create_venue_title)
        currentDestination?.hasRoute<CreateCustomVenue>() == true -> stringResource(R.string.create_custom_venue_title)
        currentDestination?.hasRoute<MusicBrainzSearch>() == true -> stringResource(R.string.musicbrainz_search_title)
        currentDestination?.hasRoute<OpenOpusComposerSearch>() == true -> stringResource(R.string.open_opus_composer_search_title)
        currentDestination?.hasRoute<OpenOpusWorkSearch>() == true -> stringResource(R.string.open_opus_work_list_title)
        currentDestination?.hasRoute<SetListEntryEdit>() == true -> {
            val isNew = currentBackStackEntry?.toRoute<SetListEntryEdit>()?.entryId == null
            if (isNew) stringResource(R.string.set_list_entry_form_add_title)
            else stringResource(R.string.set_list_entry_form_edit_title)
        }
        else -> ""
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .windowInsetsPadding(WindowInsets.navigationBars),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = {

                                if (currentDestination?.hasRoute<Settings>() == true) {
                                    // On Settings, route through the back dispatcher so the screen's BackHandler can gate navigation-away (validate before leaving).
                                    backDispatcher?.onBackPressed()
                                } else {
                                    navController.popBackStack()
                                }
                            }) {
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
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Performances,
                modifier = Modifier.padding(innerPadding)
            ) {
                performances(navController, onTabChanged = { currentTabIndex = it })
                performanceDetail()
                performanceEdit(navController)
                setListEntryEdit(navController)
                venueSearch(navController)
                createCustomVenue(navController)
                musicBrainzSearch(navController)
                openOpusComposerSearch(navController)
                openOpusWorkSearch(navController)
                settings(navController)
            }
        }
    }
}
