package com.example.expirationtracker

import com.example.expirationtracker.data.api.ApiResponse
import com.example.expirationtracker.data.api.ItemApiController
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
 * Strict OpenAPI REST Contract Test Suite for /api/v1/settings (TEST-GATE).
 *
 * Verifies exact alignment with spec/openapi.yaml:
 * 1. GET /api/v1/settings: Returns 200 OK with Settings object (default lead time = 7).
 * 2. PUT /api/v1/settings (Valid): Returns 200 OK when updating notificationLeadTime to 1, 3, 7, or 14.
 * 3. PUT /api/v1/settings (Invalid): Returns 400 Bad Request with ErrorResponse schema for unsupported lead time values (e.g. 5, 0, -1).
 *
 * CRITICAL TDD RULE: Fails on compilation because Settings model and Settings API controller methods do not exist yet.
 */
class SettingsApiContractTest {

    private lateinit var apiController: ItemApiController

    @Before
    fun setUp() {
        apiController = ItemApiController()
    }

    @Test
    fun get_settings_returns_200_ok_with_default_lead_time_7() = runBlocking {
        // openapi.yaml: GET /settings -> 200 OK with Settings schema (default 7)
        val response: ApiResponse<Settings> = apiController.getSettings()

        assertEquals("Expected HTTP 200 OK", 200, response.status)
        assertTrue(response.isSuccessful)
        assertNull(response.error)
        assertNotNull(response.data)
        assertEquals(7, response.data!!.notificationLeadTime)
    }

    @Test
    fun put_settings_valid_lead_times_returns_200_ok() = runBlocking {
        // openapi.yaml: PUT /settings with valid lead times [1, 3, 7, 14] -> 200 OK
        val validLeadTimes = listOf(1, 3, 7, 14)
        for (leadTime in validLeadTimes) {
            val requestBody = Settings(notificationLeadTime = leadTime)
            val response: ApiResponse<Settings> = apiController.updateSettings(requestBody)

            assertEquals("Expected HTTP 200 OK for lead time $leadTime", 200, response.status)
            assertTrue(response.isSuccessful)
            assertNull(response.error)
            assertEquals(leadTime, response.data?.notificationLeadTime)
        }
    }

    @Test
    fun put_settings_invalid_lead_time_returns_400_bad_request_with_ErrorResponse() = runBlocking {
        // openapi.yaml: PUT /settings with invalid value (e.g. 5, 0, -1) -> 400 Bad Request
        val invalidValues = listOf(5, 0, -1, 10, 30)
        for (invalidValue in invalidValues) {
            val requestBody = Settings(notificationLeadTime = invalidValue)
            val response: ApiResponse<Settings> = apiController.updateSettings(requestBody)

            assertEquals("Expected HTTP 400 Bad Request for invalid lead time $invalidValue", 400, response.status)
            assertFalse(response.isSuccessful)
            assertNull(response.data)

            val error = response.error
            assertNotNull("ErrorResponse body must be present", error)
            assertEquals(400, error!!.status)
            assertEquals("Bad Request", error.error)
            assertEquals("/api/v1/settings", error.path)
            assertTrue("Message must specify allowed lead times [1, 3, 7, 14]", error.message.contains("notificationLeadTime"))
            assertNotNull("Timestamp must be present", error.timestamp)
        }
    }
}
