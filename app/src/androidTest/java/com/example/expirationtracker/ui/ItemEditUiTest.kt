package com.example.expirationtracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expirationtracker.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Test Suite for Item Editing (REQ-F-012, AC-F-012).
 *
 * Traceability:
 * - Edit button exists on item cards.
 * - Submitting edit form updates the list instantly.
 *
 * CRITICAL TDD RULE: Fails in Red Phase (TEST-GATE) because edit button and edit dialog do not exist yet.
 */
@RunWith(AndroidJUnit4::class)
class ItemEditUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verify_edit_action_button_exists_and_editing_item_updates_list() {
        // Given an item exists in the list
        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Initial Item")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2027-01-01")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        composeTestRule.onNodeWithText("Initial Item").assertIsDisplayed()

        // Then an edit action button exists for the item
        composeTestRule.onNodeWithContentDescription("Edit Initial Item").assertIsDisplayed()

        // When user clicks edit, modifies title, and saves
        composeTestRule.onNodeWithContentDescription("Edit Initial Item").performClick()

        composeTestRule.onNodeWithTag("input_title").performTextClearance()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Initial Item Edited")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        // Then list updates instantly with edited item details (AC-F-012)
        composeTestRule.onNodeWithText("Initial Item Edited").assertIsDisplayed()
    }
}
