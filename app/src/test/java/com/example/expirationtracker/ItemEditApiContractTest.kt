package com.example.expirationtracker

import com.example.expirationtracker.data.api.ApiResponse
import com.example.expirationtracker.data.api.ItemApiController
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Strict OpenAPI REST Contract Test Suite for PUT /api/v1/items/{id} (Item Editing) (TEST-GATE).
 *
 * Traceability:
 * - openapi.yaml: PUT /items/{id} (200 OK, 400 Bad Request, 404 Not Found)
 * - REQ-F-012, AC-F-012.
 *
 * CRITICAL TDD RULE: Fails on compilation because updateItem API endpoint does not exist yet.
 */
class ItemEditApiContractTest {

    private lateinit var apiController: ItemApiController

    @Before
    fun setUp() {
        apiController = ItemApiController()
    }

    @Test
    fun put_item_valid_returns_200_ok_with_updated_item() = runBlocking {
        // Given an existing item
        val createResp = apiController.createItem(
            ItemInput("Original Title", "2027-01-01", Category.FOOD, "Pantry")
        )
        val createdItem = createResp.data
        assertNotNull(createdItem)
        val itemId = createdItem!!.id

        // When updating item with valid input
        val updateInput = ItemInput(
            title = "Updated Title",
            expirationDate = "2027-02-01",
            category = Category.MEDICINE,
            storageLocation = "Cabinet"
        )
        val response: ApiResponse<Item> = apiController.updateItem(itemId, updateInput)

        assertEquals("Expected HTTP 200 OK", 200, response.status)
        assertTrue(response.isSuccessful)
        assertNull(response.error)

        val updatedItem = response.data
        assertNotNull(updatedItem)
        assertEquals(itemId, updatedItem!!.id)
        assertEquals("Updated Title", updatedItem.title)
        assertEquals("2027-02-01", updatedItem.expirationDate)
        assertEquals(Category.MEDICINE, updatedItem.category)
        assertEquals("Cabinet", updatedItem.storageLocation)
    }

    @Test
    fun put_item_invalid_empty_title_returns_400_bad_request_with_ErrorResponse() = runBlocking {
        val createResp = apiController.createItem(
            ItemInput("Valid Title", "2027-01-01", Category.FOOD)
        )
        val itemId = createResp.data!!.id

        val invalidInput = ItemInput(
            title = "   ",
            expirationDate = "2027-01-01",
            category = Category.FOOD
        )
        val response: ApiResponse<Item> = apiController.updateItem(itemId, invalidInput)

        assertEquals("Expected HTTP 400 Bad Request", 400, response.status)
        assertFalse(response.isSuccessful)
        assertNull(response.data)

        val error = response.error
        assertNotNull(error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/items/$itemId", error.path)
        assertTrue(error.message.contains("title", ignoreCase = true))
    }

    @Test
    fun put_item_non_existent_id_returns_404_not_found_with_ErrorResponse() = runBlocking {
        val nonExistentId = UUID.randomUUID().toString()
        val updateInput = ItemInput(
            title = "Some Title",
            expirationDate = "2027-01-01",
            category = Category.FOOD
        )

        val response: ApiResponse<Item> = apiController.updateItem(nonExistentId, updateInput)

        assertEquals("Expected HTTP 404 Not Found", 404, response.status)
        assertFalse(response.isSuccessful)
        assertNull(response.data)

        val error = response.error
        assertNotNull(error)
        assertEquals(404, error!!.status)
        assertEquals("Not Found", error.error)
        assertEquals("/api/v1/items/$nonExistentId", error.path)
        assertTrue(error.message.contains(nonExistentId) || error.message.contains("not found", ignoreCase = true))
    }
}
