package com.example.expirationtracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expirationtracker.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI BDD Test Suite for TSK-006: Item Deletion (REQ-F-011, AC-F-011).
 *
 * Traceability:
 * - REQ-F-011: Allow user to manually delete an existing item from local DB.
 * - AC-F-011: Item deletion permanently removes item and immediately disappears visually from screen.
 */
@RunWith(AndroidJUnit4::class)
class ItemDeletionUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verify_delete_action_exists_for_items_in_list() {
        // Given an item exists in the list
        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Delete Test Item")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2027-05-15")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        // Verify item is displayed
        composeTestRule.onNodeWithText("Delete Test Item").assertIsDisplayed()

        // Then verify that a delete action (icon button with content description or tag) exists for the item
        composeTestRule.onNodeWithContentDescription("Delete Delete Test Item")
            .assertIsDisplayed()
    }

    @Test
    fun bdd_AC_F_011_triggering_delete_action_removes_item_visually_immediately() {
        // Given an item exists in the list
        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Temporary Item")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2027-06-20")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        composeTestRule.onNodeWithText("Temporary Item").assertIsDisplayed()

        // When the user triggers the delete action for that item
        composeTestRule.onNodeWithContentDescription("Delete Temporary Item").performClick()

        // Then the item is permanently removed and immediately disappears visually from the screen (AC-F-011)
        composeTestRule.onNodeWithText("Temporary Item").assertDoesNotExist()
    }
}
