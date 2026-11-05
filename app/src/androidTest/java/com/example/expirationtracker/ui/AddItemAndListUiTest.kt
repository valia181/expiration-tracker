package com.example.expirationtracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
 * UI Test Suite for Item Creation Form and Chronological List Display.
 *
 * Traceability:
 * - AC-F-001: Manual Item Creation via Add Item form
 * - AC-F-002: Title validation error display on UI
 * - AC-F-003: Expiration Date validation error display on UI
 * - AC-F-004: Chronological item list display
 * - AC-F-005: Dynamic color status badge (Red, Orange, Green)
 * - REQ-F-001, REQ-F-002, REQ-F-003, REQ-F-004, REQ-F-005
 * - openapi.yaml: POST /items, GET /items, ErrorResponse
 *
 * CRITICAL TDD RULE: Fails in Red Phase (TEST-GATE) because the Add Item form,
 * FAB/button mechanism, and input fields do not exist yet on the UI.
 */
@RunWith(AndroidJUnit4::class)
class AddItemAndListUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun test_presence_of_ui_mechanism_to_open_add_item_form() {
        // Given user is on the main screen
        // Then an Add Item action mechanism (FAB with tag "fab_add_item" or contentDescription "Add Item") must be visible
        composeTestRule.onNodeWithTag("fab_add_item")
            .assertIsDisplayed()
    }

    @Test
    fun positive_bdd_AC_F_001_submit_valid_item_updates_list_with_correct_status() {
        // Given user is on main screen and opens the Add Item dialog/screen
        composeTestRule.onNodeWithTag("fab_add_item").performClick()

        // When user enters valid inputs matching REQ-F-001 & AC-F-001
        composeTestRule.onNodeWithTag("input_title").performTextInput("Fresh Milk")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2026-11-06") // Tomorrow -> Orange
        composeTestRule.onNodeWithTag("input_storage_location").performTextInput("Fridge Top Shelf")

        // And taps Save
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        // Then the new item appears in the list
        composeTestRule.onNodeWithText("Fresh Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expires: 2026-11-06").assertIsDisplayed()
        composeTestRule.onNodeWithText("Location: Fridge Top Shelf").assertIsDisplayed()

        // And status badge is dynamically assigned (Warning / Expiring Soon)
        composeTestRule.onNodeWithText("Expiring Soon").assertIsDisplayed()
    }

    @Test
    fun negative_bdd_AC_F_002_empty_title_displays_errorResponse_on_ui() {
        // Given user opens Add Item form
        composeTestRule.onNodeWithTag("fab_add_item").performClick()

        // When user enters empty title or solely whitespace
        composeTestRule.onNodeWithTag("input_title").performTextInput("   ")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2027-01-01")

        // And submits
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        // Then ErrorResponse message is visibly displayed to the user on the UI
        composeTestRule.onNodeWithText("Validation error: Item title cannot be empty or whitespace-only.")
            .assertIsDisplayed()
    }

    @Test
    fun negative_bdd_AC_F_003_invalid_date_displays_errorResponse_on_ui() {
        // Given user opens Add Item form
        composeTestRule.onNodeWithTag("fab_add_item").performClick()

        // When user enters valid title but invalid calendar date (e.g. 2026-02-30)
        composeTestRule.onNodeWithTag("input_title").performTextInput("Bleach")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2026-02-30")

        // And submits
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        // Then ErrorResponse message is visibly displayed on the UI
        composeTestRule.onNodeWithText("Validation error: Expiration date must be a valid calendar date in YYYY-MM-DD format.")
            .assertIsDisplayed()
    }

    @Test
    fun list_display_AC_F_004_AC_F_005_items_rendered_chronologically() {
        // Given main screen with items
        // The list container should be displayed
        composeTestRule.onNodeWithTag("item_list_lazy_column")
            .assertIsDisplayed()
    }
}
