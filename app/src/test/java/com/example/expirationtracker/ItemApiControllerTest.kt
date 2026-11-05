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
 * Controller-level REST API boundary Unit Test Suite (TEST-GATE).
 *
 * Verifies OpenAPI contract compliance for:
 * 1. POST Positive: Valid ItemInput returns HTTP 201 Created with generated UUID and calculated statusColor.
 * 2. POST Negative (Empty Title): Returns HTTP 400 Bad Request matching ErrorResponse schema with 'title'.
 * 3. POST Negative (Invalid Date): Returns HTTP 400 Bad Request for non-existent dates ('2026-02-30').
 * 4. GET Positive: Returns HTTP 200 OK with JSON array of Item objects sorted chronologically.
 * 5. GET Negative (Invalid Category): Returns HTTP 400 Bad Request and ErrorResponse for invalid category filter ('Toys').
 */
class ItemApiControllerTest {

    private lateinit var controller: ItemApiController

    @Before
    fun setUp() {
        controller = ItemApiController()
    }

    @Test
    fun post_positive_valid_itemInput_returns_201_created_with_uuid_and_statusColor() = runBlocking {
        // openapi.yaml: POST /items -> 201 Created
        val input = ItemInput(
            title = "Aspirin 500mg",
            expirationDate = "2027-01-01",
            category = Category.MEDICINE,
            storageLocation = "Cabinet"
        )

        val response: ApiResponse<Item> = controller.createItem(input)

        assertEquals("Expected HTTP 201 Created", 201, response.status)
        assertTrue("Response should indicate success", response.isSuccessful)
        assertNull("ErrorResponse must be null for 201 Created", response.error)

        val item = response.data
        assertNotNull("Response data must contain created item", item)
        assertEquals("Aspirin 500mg", item!!.title)
        assertEquals("2027-01-01", item.expirationDate)
        assertEquals(Category.MEDICINE, item.category)
        assertEquals("Cabinet", item.storageLocation)

        // Verify generated UUID
        assertNotNull(UUID.fromString(item.id))

        // Verify calculated statusColor
        assertNotNull(item.statusColor)
    }

    @Test
    fun post_negative_empty_title_returns_400_bad_request_with_errorResponse() = runBlocking {
        // openapi.yaml: POST /items 400 Bad Request
        val input = ItemInput(
            title = "   ",
            expirationDate = "2027-01-01",
            category = Category.MEDICINE
        )

        val response: ApiResponse<Item> = controller.createItem(input)

        assertEquals("Expected HTTP 400 Bad Request", 400, response.status)
        assertFalse("Response must indicate failure", response.isSuccessful)
        assertNull("Data must be null for 400 Bad Request", response.data)

        val error = response.error
        assertNotNull("ErrorResponse body must be present", error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/items", error.path)
        assertTrue("Message must contain 'title'", error.message.contains("title", ignoreCase = true))
        assertNotNull("Timestamp must be populated", error.timestamp)
    }

    @Test
    fun post_negative_invalid_date_returns_400_bad_request_with_errorResponse() = runBlocking {
        // Non-existent calendar date (2026-02-30)
        val input = ItemInput(
            title = "Bleach",
            expirationDate = "2026-02-30",
            category = Category.CHEMICALS
        )

        val response: ApiResponse<Item> = controller.createItem(input)

        assertEquals("Expected HTTP 400 Bad Request for non-existent date", 400, response.status)
        assertFalse(response.isSuccessful)
        assertNull(response.data)

        val error = response.error
        assertNotNull("ErrorResponse body must be present", error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/items", error.path)
        assertTrue("Message must indicate date error", error.message.contains("date", ignoreCase = true))
    }

    @Test
    fun get_positive_returns_200_ok_with_chronologically_sorted_items() = runBlocking {
        // Seed items
        controller.createItem(ItemInput("Far Future Item", "2028-12-31", Category.FOOD))
        controller.createItem(ItemInput("Soon Expiring Item", "2026-11-10", Category.MEDICINE))
        controller.createItem(ItemInput("Expired Item", "2026-11-01", Category.COSMETICS))

        val response: ApiResponse<List<Item>> = controller.getItems(search = null, category = null)

        assertEquals("Expected HTTP 200 OK", 200, response.status)
        assertTrue(response.isSuccessful)
        assertNull(response.error)

        val items = response.data
        assertNotNull("Response body must contain items array", items)
        assertTrue("Must contain items", items!!.size >= 3)

        // Verify chronological ascending order
        for (i in 0 until items.size - 1) {
            assertTrue(
                "Items must be sorted chronologically ascending",
                items[i].expirationDate <= items[i + 1].expirationDate
            )
        }
    }

    @Test
    fun get_negative_invalid_category_returns_400_bad_request_with_errorResponse() = runBlocking {
        // openapi.yaml: Invalid category query parameter
        val response: ApiResponse<List<Item>> = controller.getItems(search = null, category = "Toys")

        assertEquals("Expected HTTP 400 Bad Request for invalid category", 400, response.status)
        assertFalse(response.isSuccessful)
        assertNull("Data must be null on 400 error", response.data)

        val error = response.error
        assertNotNull("ErrorResponse body must be present", error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/items", error.path)
        assertTrue(
            "Message must mention 'Toys' and allowed categories",
            error.message.contains("Toys") && error.message.contains("Food, Medicine, Cosmetics, Chemicals")
        )
    }
}
