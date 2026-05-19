package com.chaddy50.concerttracker.ui.performanceDetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.chaddy50.concerttracker.TestData
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme
import org.junit.Rule
import org.junit.Test

class SetListEntryCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setListEntryCard_showsConductorWhenDifferentFromPerformanceConductor() {
        val entryWithConductor = TestData.setListEntry.copy(conductor = TestData.conductor)
        composeTestRule.setContent {
            ConcertTrackerTheme {
                SetListEntryCard(entry = entryWithConductor, performanceConductorId = "different-id", draftNotes = "", onNotesChange = {})
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertIsDisplayed()
    }

    @Test
    fun setListEntryCard_hidesConductorWhenSameAsPerformanceConductor() {
        val entryWithConductor = TestData.setListEntry.copy(conductor = TestData.conductor)
        composeTestRule.setContent {
            ConcertTrackerTheme {
                SetListEntryCard(entry = entryWithConductor, performanceConductorId = TestData.conductor.id, draftNotes = "", onNotesChange = {})
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertDoesNotExist()
    }

    @Test
    fun setListEntryCard_showsConductorWhenPerformanceHasNoConductor() {
        val entryWithConductor = TestData.setListEntry.copy(conductor = TestData.conductor)
        composeTestRule.setContent {
            ConcertTrackerTheme {
                SetListEntryCard(entry = entryWithConductor, performanceConductorId = null, draftNotes = "", onNotesChange = {})
            }
        }
        composeTestRule.onNodeWithText("Simon Rattle, conductor").assertIsDisplayed()
    }
}