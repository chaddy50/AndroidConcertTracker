package com.chaddy50.concerttracker.ui.performances

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.chaddy50.concerttracker.TestData
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import org.junit.Rule
import org.junit.Test

class PerformancesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun performancesContent_showsErrorMessage() {
        composeTestRule.setContent {
            ConcertTrackerTheme {
                PerformancesContent(
                    uiState = PerformancesUiState.Error("Could not connect to server"),
                    onPerformanceClick = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Could not connect to server").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun performancesContent_showsPerformanceList() {
        composeTestRule.setContent {
            ConcertTrackerTheme {
                PerformancesContent(
                    uiState = PerformancesUiState.Success(listOf(TestData.performance)),
                    onPerformanceClick = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Royal Albert Hall").assertIsDisplayed()
    }
}