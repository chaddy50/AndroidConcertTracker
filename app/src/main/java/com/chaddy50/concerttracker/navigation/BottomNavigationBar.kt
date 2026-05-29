package com.chaddy50.concerttracker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.chaddy50.concerttracker.navigation.routes.HomeTab
import com.chaddy50.concerttracker.navigation.routes.PastTab
import com.chaddy50.concerttracker.navigation.routes.UpcomingTab

private data class TabItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

private val tabs = listOf(
    TabItem("Home", Icons.Default.Home, HomeTab),
    TabItem("Upcoming", Icons.Default.DateRange, UpcomingTab),
    TabItem("Past", Icons.AutoMirrored.Default.List, PastTab)
)

@Composable
fun BottomNavigationBar(tabNavController: NavHostController) {
    val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    NavigationBar {
        tabs.forEach { tab ->
            val isSelected = when (tab.route) {
                HomeTab -> currentDestination?.hasRoute<HomeTab>() == true
                UpcomingTab -> currentDestination?.hasRoute<UpcomingTab>() == true
                PastTab -> currentDestination?.hasRoute<PastTab>() == true
                else -> false
            }
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    tabNavController.navigate(tab.route) {
                        popUpTo(tabNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
