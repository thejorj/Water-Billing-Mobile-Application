package com.example.waterbilling

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testInitialState() {
        // Verify the app title is displayed in the TopAppBar
        composeTestRule.onAllNodesWithText("DALOY")
            .assertCountEquals(2) // We expect two instances of "DALOY"
        
        // Verify the initial empty state message
        composeTestRule.onNodeWithText("Import an Excel file to view data")
            .assertExists()
            .assertIsDisplayed()
        
        // Verify the menu button exists
        composeTestRule.onNodeWithContentDescription("More options")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testMenuOptions() {
        // Open the menu
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        
        // Verify menu items exist
        composeTestRule.onNodeWithText("Import Excel").assertExists()
        composeTestRule.onNodeWithText("Export Excel").assertExists()
        composeTestRule.onNodeWithText("Restore Backup").assertExists()
        composeTestRule.onNodeWithText("Clear Data").assertExists()
    }

    @Test
    fun testAutosaveToggle() {
        // Verify autosave section exists
        composeTestRule.onNodeWithText("Autosave")
            .assertExists()
            .assertIsDisplayed()
        
        // Since the switch is disabled initially (no data loaded),
        // just verify it exists but don't check enabled state
        composeTestRule.onNode(hasTestTag("autosave_switch"))
            .assertExists()
            .assertIsDisplayed()
            .assertIsNotEnabled() // Switch should be disabled when no data is loaded
    }

    @Test
    fun testClearDataConfirmation() {
        // Open the menu
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        
        // Click Clear Data
        composeTestRule.onNodeWithText("Clear Data").performClick()
        
        // Verify confirmation dialog appears
        composeTestRule.onNodeWithText("Clear All Data?").assertExists()
        composeTestRule.onNodeWithText("This action cannot be undone!").assertExists()
        
        // Verify dialog buttons
        composeTestRule.onNodeWithText("Backup & Clear").assertExists()
        composeTestRule.onNodeWithText("Clear Without Backup").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }
} 