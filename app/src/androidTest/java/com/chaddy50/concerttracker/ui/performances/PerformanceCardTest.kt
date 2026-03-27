package com.chaddy50.concerttracker.ui.performances

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.chaddy50.concerttracker.TestData
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PerformanceCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun performanceCard_showsConductorWhenPresent() {
        composeTestRule.setContent {
            ConcertTrackerTheme {
                PerformanceCard(performance = TestData.performance, onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertIsDisplayed()
    }

    @Test
    fun performanceCard_doesNotShowConductorWhenAbsent() {
        composeTestRule.setContent {
            ConcertTrackerTheme {
                PerformanceCard(performance = TestData.performanceWithoutConductor, onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertDoesNotExist()
    }

    @Test
    fun performanceCard_callsOnClickWhenTapped() {
        var clicked = false
        composeTestRule.setContent {
            ConcertTrackerTheme {
                PerformanceCard(performance = TestData.performance, onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Royal Albert Hall").performClick()
        assertTrue(clicked)
    }
}