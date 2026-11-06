package com.example.expirationtracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expirationtracker.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Test Suite for Backup Export/Import (REQ-F-009, REQ-F-010, AC-F-009, AC-F-010).
 *
 * Traceability:
 * - REQ-F-009 & AC-F-009: Export Backup UI action.
 * - REQ-F-010 & AC-F-010: Import Backup UI action.
 *
 * CRITICAL TDD RULE: Fails in Red Phase (TEST-GATE) because Export and Import UI buttons/menu items do not exist yet.
 */
@RunWith(AndroidJUnit4::class)
class BackupUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verify_presence_of_export_backup_and_import_backup_ui_actions() {
        // Given user is on main screen
        // Then Export Backup and Import Backup action buttons/menu items exist on UI
        composeTestRule.onNodeWithTag("btn_export_backup").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_import_backup").assertIsDisplayed()
    }
}
