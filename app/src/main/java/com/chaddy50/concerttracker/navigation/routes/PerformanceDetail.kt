package com.chaddy50.concerttracker.navigation.routes

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chaddy50.concerttracker.ui.screens.performanceDetailScreen.PerformanceDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class PerformanceDetail(val id: String)

fun NavGraphBuilder.performanceDetail() {
    composable<PerformanceDetail> {
        PerformanceDetailScreen()
    }
}