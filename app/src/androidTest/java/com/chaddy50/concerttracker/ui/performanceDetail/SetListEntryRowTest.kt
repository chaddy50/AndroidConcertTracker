package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.chaddy50.concerttracker.TestData
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import org.junit.Rule
import org.junit.Test

class SetListEntryRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setListEntryRow_showsConductorWhenDifferentFromPerformanceConductor() {
        val entryWithConductor = TestData.setListEntry.copy(conductor = TestData.conductor)
        composeTestRule.setContent {
            ConcertTrackerTheme {
                SetListEntryRow(entry = entryWithConductor, performanceConductorId = "different-id")
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertIsDisplayed()
    }

    @Test
    fun setListEntryRow_hidesConductorWhenSameAsPerformanceConductor() {
        val entryWithConductor = TestData.setListEntry.copy(conductor = TestData.conductor)
        composeTestRule.setContent {
            ConcertTrackerTheme {
                SetListEntryRow(entry = entryWithConductor, performanceConductorId = TestData.conductor.id)
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertDoesNotExist()
    }

    @Test
    fun setListEntryRow_showsConductorWhenPerformanceHasNoConductor() {
        val entryWithConductor = TestData.setListEntry.copy(conductor = TestData.conductor)
        composeTestRule.setContent {
            ConcertTrackerTheme {
                SetListEntryRow(entry = entryWithConductor, performanceConductorId = null)
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertIsDisplayed()
    }
}