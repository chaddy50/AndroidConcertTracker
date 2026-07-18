package com.chaddy50.concerttracker.navigation.topBarActions

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.chaddy50.concerttracker.navigation.routes.PerformanceDetail
import com.chaddy50.concerttracker.navigation.routes.PerformanceEdit
import com.chaddy50.concerttracker.navigation.routes.Performances
import com.chaddy50.concerttracker.navigation.routes.SetListEntryEdit
import com.chaddy50.concerttracker.navigation.routes.Settings
import com.chaddy50.concerttracker.ui.screens.editPerformanceScreen.EditPerformanceViewModel
import com.chaddy50.concerttracker.ui.screens.editSetListEntryScreen.EditSetListEntryViewModel

@Composable
fun TopBarActionsRouter(
    currentDestination: NavDestination?,
    currentBackStackEntry: NavBackStackEntry?,
    navController: NavController
) {
    when {
        currentDestination?.hasRoute<Performances>() == true -> {
            PerformancesTopBarActions(
                onNavigateToSettings = { navController.navigate(Settings) }
            )
        }
        currentDestination?.hasRoute<PerformanceDetail>() == true -> {
            currentBackStackEntry?.let { entry ->
                val performanceId = entry.toRoute<PerformanceDetail>().id
                PerformanceDetailTopBarActions(
                    onNavigateToEdit = { navController.navigate(PerformanceEdit(performanceId)) }
                )
            }
        }
        currentDestination?.hasRoute<PerformanceEdit>() == true -> {
            currentBackStackEntry?.let { entry ->
                val performanceId = entry.toRoute<PerformanceEdit>().id
                if (performanceId != null) {
                    val viewModel: EditPerformanceViewModel = hiltViewModel(entry)
                    EditPerformanceTopBarActions(
                        onDeletePerformance = {
                            viewModel.deletePerformance {
                                navController.popBackStack<Performances>(inclusive = false)
                            }
                        }
                    )
                }
            }
        }
        currentDestination?.hasRoute<SetListEntryEdit>() == true -> {
            currentBackStackEntry?.let { entry ->
                val entryId = entry.toRoute<SetListEntryEdit>().entryId
                if (entryId != null) {
                    val viewModel: EditSetListEntryViewModel = hiltViewModel(entry)
                    EditSetListEntryTopBarActions(
                        onDeleteSetListEntry = {
                            viewModel.deleteSetListEntry {
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }
}
