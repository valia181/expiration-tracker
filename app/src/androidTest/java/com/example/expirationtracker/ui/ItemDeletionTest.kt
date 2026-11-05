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
 * UI Test Suite for Item Deletion Confirmation Dialog (AC-F-011, REQ-F-011).
 *
 * Traceability:
 * - Initiating delete action shows confirmation dialog instead of immediate deletion.
 * - Clicking Cancel dismisses dialog without deleting.
 * - Clicking Confirm triggers deletion, removes item, and updates list dynamically.
 *
 * CRITICAL TDD RULE: Fails in Red Phase (TEST-GATE) because confirmation dialog does not exist yet.
 */
@RunWith(AndroidJUnit4::class)
class ItemDeletionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun initiating_delete_shows_confirmation_dialog_and_cancel_preserves_item() {
        // Given an item exists in the list
        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Item to Keep")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2027-07-01")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        composeTestRule.onNodeWithText("Item to Keep").assertIsDisplayed()

        // When user clicks delete action
        composeTestRule.onNodeWithContentDescription("Delete Item to Keep").performClick()

        // Then a confirmation dialog appears asking for confirmation
        composeTestRule.onNodeWithText("Are you sure you want to delete this item?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_confirm_delete").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_cancel_delete").assertIsDisplayed()

        // And item is still visible (NOT immediately deleted)
        composeTestRule.onNodeWithText("Item to Keep").assertIsDisplayed()

        // When user clicks Cancel
        composeTestRule.onNodeWithTag("btn_cancel_delete").performClick()

        // Then dialog is dismissed and item remains in the list
        composeTestRule.onNodeWithText("Are you sure you want to delete this item?").assertDoesNotExist()
        composeTestRule.onNodeWithText("Item to Keep").assertIsDisplayed()
    }

    @Test
    fun confirming_delete_dialog_deletes_item_permanently() {
        // Given an item exists and delete confirmation dialog is open
        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Item to Delete Confirm")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2027-08-01")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        composeTestRule.onNodeWithText("Item to Delete Confirm").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Delete Item to Delete Confirm").performClick()
        composeTestRule.onNodeWithText("Are you sure you want to delete this item?").assertIsDisplayed()

        // When user clicks Confirm
        composeTestRule.onNodeWithTag("btn_confirm_delete").performClick()

        // Then dialog closes and item is permanently removed from the screen immediately
        composeTestRule.onNodeWithText("Are you sure you want to delete this item?").assertDoesNotExist()
        composeTestRule.onNodeWithText("Item to Delete Confirm").assertDoesNotExist()
    }
}
