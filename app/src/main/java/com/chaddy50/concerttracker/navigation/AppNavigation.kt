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
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.ui.performanceDetail.PerformanceDetailScreen
import com.chaddy50.concerttracker.ui.performanceEdit.PerformanceEditScreen
import com.chaddy50.concerttracker.ui.performances.PerformancesScreen
import com.chaddy50.concerttracker.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object Performances

@Serializable
object Settings

@Serializable
data class PerformanceDetail(val id: String)

@Serializable
data class PerformanceEdit(val id: String?)

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
            composable<PerformanceEdit> {
                PerformanceEditScreen(
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
