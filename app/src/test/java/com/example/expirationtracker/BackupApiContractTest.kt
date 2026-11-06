package com.example.expirationtracker

import com.example.expirationtracker.data.api.ApiResponse
import com.example.expirationtracker.data.api.ItemApiController
import com.example.expirationtracker.domain.model.BackupData
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.model.Settings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Strict OpenAPI REST Contract Test Suite for Backup Export/Import (/api/v1/backups) (TEST-GATE).
 *
 * Traceability:
 * - openapi.yaml: POST /api/v1/backups/export, POST /api/v1/backups/import
 * - REQ-F-009, REQ-F-010, REQ-NF-003, AC-F-009, AC-F-010.
 *
 * CRITICAL TDD RULE: Fails on compilation because BackupData model and export/import controller methods do not exist yet.
 */
class BackupApiContractTest {

    private lateinit var apiController: ItemApiController

    @Before
    fun setUp() {
        apiController = ItemApiController()
    }

    @Test
    fun export_backup_returns_200_ok_with_valid_BackupData_schema() = runBlocking {
        // Seed items and update settings
        apiController.createItem(ItemInput("Export Milk", "2026-11-20", Category.FOOD))
        apiController.updateSettings(Settings(notificationLeadTime = 14))

        // openapi.yaml: POST /backups/export -> 200 OK with BackupData (settings + items)
        val response: ApiResponse<BackupData> = apiController.exportBackup()

        assertEquals("Expected HTTP 200 OK for backup export", 200, response.status)
        assertTrue(response.isSuccessful)
        assertNull(response.error)

        val backupData = response.data
        assertNotNull("Response body must contain BackupData object", backupData)
        assertNotNull("BackupData must contain settings object", backupData!!.settings)
        assertNotNull("BackupData must contain items array", backupData.items)
        assertEquals(14, backupData.settings.notificationLeadTime)
        assertTrue("Backup items must contain seeded item", backupData.items.any { it.title == "Export Milk" })
    }

    @Test
    fun import_backup_valid_data_returns_200_ok_and_replaces_database_data() = runBlocking {
        // Seed initial data
        apiController.createItem(ItemInput("Old Bread", "2026-11-10", Category.FOOD))

        // Create backup data to import
        val importItemInput = ItemInput("Imported Cheese", "2027-05-01", Category.FOOD)
        val createResp = apiController.createItem(importItemInput)
        val importedItem = createResp.data!!

        val validBackupData = BackupData(
            settings = Settings(notificationLeadTime = 3),
            items = listOf(importedItem)
        )

        // openapi.yaml: POST /backups/import -> 200 OK
        val response: ApiResponse<String> = apiController.importBackup(validBackupData)

        assertEquals("Expected HTTP 200 OK for backup import", 200, response.status)
        assertTrue(response.isSuccessful)

        // Verify database replaced with imported backup
        val currentItems = apiController.getItems().data
        assertNotNull(currentItems)
        assertEquals(1, currentItems!!.size)
        assertEquals("Imported Cheese", currentItems[0].title)

        val currentSettings = apiController.getSettings().data
        assertEquals(3, currentSettings?.notificationLeadTime)
    }

    @Test
    fun import_backup_invalid_corrupted_schema_returns_400_bad_request_with_ErrorResponse() = runBlocking {
        // Corrupted / invalid backup data e.g. invalid settings lead time (not 1, 3, 7, 14)
        val invalidBackupData = BackupData(
            settings = Settings(notificationLeadTime = 99),
            items = emptyList()
        )

        // openapi.yaml: POST /backups/import with invalid schema -> 400 Bad Request
        val response: ApiResponse<String> = apiController.importBackup(invalidBackupData)

        assertEquals("Expected HTTP 400 Bad Request for corrupted backup import", 400, response.status)
        assertFalse(response.isSuccessful)
        assertNull(response.data)

        val error = response.error
        assertNotNull("Body must contain ErrorResponse", error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/backups/import", error.path)
        assertTrue("Message must indicate invalid backup file format or settings", error.message.contains("Invalid backup", ignoreCase = true) || error.message.contains("notificationLeadTime", ignoreCase = true))
        assertNotNull(error.timestamp)
    }

    @Test
    fun import_backup_failure_rolls_back_database_transactionally_REQ_NF_003() = runBlocking {
        // Seed initial valid items
        apiController.createItem(ItemInput("Original Item 1", "2026-11-20", Category.FOOD))
        apiController.createItem(ItemInput("Original Item 2", "2026-12-01", Category.MEDICINE))
        val originalCount = apiController.getItems().data?.size ?: 0

        // Attempt import with corrupted data
        val corruptedBackup = BackupData(
            settings = Settings(notificationLeadTime = -5),
            items = emptyList()
        )

        val importResponse = apiController.importBackup(corruptedBackup)
        assertEquals(400, importResponse.status)

        // Transactional integrity check (REQ-NF-003): DB state must be rolled back and preserved
        val itemsAfterFailedImport = apiController.getItems().data
        assertNotNull(itemsAfterFailedImport)
        assertEquals("Database must roll back to previous state on failed import", originalCount, itemsAfterFailedImport!!.size)
    }
}
