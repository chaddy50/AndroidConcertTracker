package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.chaddy50.concerttracker.TestData
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import org.junit.Rule
import org.junit.Test

class PerformanceDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun performanceDetailContent_showsErrorMessage() {
        composeTestRule.setContent {
            ConcertTrackerTheme {
                PerformanceDetailContent(
                    uiState = PerformanceDetailUiState.Error("Could not load performance"),
                    onRetry = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Could not load performance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun performanceDetailContent_showsPerformanceDetails() {
        composeTestRule.setContent {
            ConcertTrackerTheme {
                PerformanceDetailContent(
                    uiState = PerformanceDetailUiState.Success(TestData.performance),
                    onRetry = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Royal Albert Hall").assertIsDisplayed()
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertIsDisplayed()
    }
}