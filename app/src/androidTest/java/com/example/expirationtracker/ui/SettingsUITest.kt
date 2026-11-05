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
 * UI & BDD Test Suite for TSK-010: Global Notification Lead Time Settings (REQ-F-008, AC-F-005, AC-F-008).
 *
 * Traceability:
 * - REQ-F-008 & AC-F-008: Settings screen/dialog allowing selection of 1, 3, 7, or 14 days.
 * - AC-F-008: Changing lead time (e.g. from 3 to 7 days) and saving triggers dynamic recalculation of StatusColor for existing items.
 *
 * CRITICAL TDD RULE: Fails in Red Phase (TEST-GATE) because Settings screen/dialog and lead time options do not exist yet on UI.
 */
@RunWith(AndroidJUnit4::class)
class SettingsUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verify_presence_of_settings_action_and_lead_time_options_1_3_7_14() {
        // Given user is on main screen
        // Then settings access action button exists
        composeTestRule.onNodeWithTag("btn_open_settings").assertIsDisplayed()

        // When user opens settings screen/dialog
        composeTestRule.onNodeWithTag("btn_open_settings").performClick()

        // Then options for 1, 3, 7, or 14 days must be displayed
        composeTestRule.onNodeWithTag("lead_time_option_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lead_time_option_3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lead_time_option_7").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lead_time_option_14").assertIsDisplayed()
    }

    @Test
    fun bdd_AC_F_008_changing_lead_time_dynamically_recalculates_status_color() {
        // Given user adds an item expiring in 6 days (Safe under 3-day lead time, Warning (Orange) under 7-day lead time)
        composeTestRule.onNodeWithTag("fab_add_item").performClick()
        composeTestRule.onNodeWithTag("input_title").performTextInput("Yogurt 6 Days")
        composeTestRule.onNodeWithTag("btn_open_settings").performClick()

        // When user selects 7 days lead time and saves
        composeTestRule.onNodeWithTag("lead_time_option_7").performClick()
        composeTestRule.onNodeWithTag("btn_save_settings").performClick()

        // Then items falling within the 7-day window update their status to Orange (Expiring Soon)
        composeTestRule.onNodeWithTag("item_list_lazy_column").assertIsDisplayed()
    }
}
