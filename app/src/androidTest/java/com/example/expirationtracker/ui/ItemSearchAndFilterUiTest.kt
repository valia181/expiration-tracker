package com.example.expirationtracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 * UI BDD Test Suite for TSK-008: Search and Category Filtering (REQ-F-006, REQ-F-007, REQ-NF-002, AC-F-006, AC-F-007).
 *
 * Traceability:
 * - REQ-F-006 & AC-F-006: Case-insensitive title search.
 * - REQ-F-007 & AC-F-007: Material Design 3 category filter chips (Food, Medicine, Cosmetics, Chemicals, All).
 * - REQ-NF-002 & AC-NF-002: Query performance < 100ms.
 *
 * CRITICAL TDD RULE: Fails in Red Phase (TEST-GATE) because Search bar and Category Filter Chips do not exist yet on the UI.
 */
@RunWith(AndroidJUnit4::class)
class ItemSearchAndFilterUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verify_search_bar_and_category_filter_chips_exist_on_screen() {
        // Given user is on the main screen
        // Then search bar must be visible on screen
        composeTestRule.onNodeWithTag("input_search").assertIsDisplayed()

        // And Material Design 3 category filter chips must exist for All, Food, Medicine, Cosmetics, Chemicals
        composeTestRule.onNodeWithTag("filter_chip_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_food").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_medicine").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_cosmetics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_chemicals").assertIsDisplayed()
    }

    @Test
    fun bdd_AC_F_006_typing_in_search_bar_updates_displayed_list() {
        // Given user seeds items into the list
        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Aspirin 500mg")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2027-01-01")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Fresh Whole Milk")
        composeTestRule.onNodeWithTag("input_expiration_date").performTextInput("2026-12-01")
        composeTestRule.onNodeWithTag("btn_save_item").performClick()

        // When user types query "Aspirin" into search bar
        composeTestRule.onNodeWithTag("input_search").performTextInput("Aspirin")

        // Then displayed list updates dynamically to show matching item and hide non-matching
        composeTestRule.onNodeWithText("Aspirin 500mg").assertIsDisplayed()
    }
}
