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
 * Strict OpenAPI REST Contract Test Suite for /api/v1/items (TEST-GATE).
 *
 * Verifies exact alignment with spec/openapi.yaml:
 * 1. POST /api/v1/items (Valid): Returns 201 Created. Body is Item with generated UUID & calculated statusColor.
 * 2. POST /api/v1/items (Invalid): Returns 400 Bad Request with ErrorResponse model (timestamp, status, error, message, path).
 * 3. GET /api/v1/items (Valid): Returns 200 OK. Body is JSON array of Items sorted chronologically. Supports search & category filters.
 * 4. GET /api/v1/items (Invalid category): Returns 400 Bad Request with ErrorResponse for unknown category (e.g. 'Toys').
 * 5. DELETE /api/v1/items/{id} (Valid): Returns 204 No Content for existing ID and permanently removes item.
 * 6. DELETE /api/v1/items/{id} (Non-existent ID): Returns 404 Not Found with ErrorResponse schema.
 *
 * CRITICAL TDD RULE: Fails on compilation because DELETE endpoint is not implemented yet.
 */
class ItemApiContractTest {

    private lateinit var apiController: ItemApiController

    @Before
    fun setUp() {
        // ItemApiController does NOT exist yet (Red Phase)
        apiController = ItemApiController()
    }

    // ==========================================
    // 1. POST /api/v1/items Contract Tests
    // ==========================================

    @Test
    fun post_item_valid_returns_201_created_with_uuid_and_statusColor() = runBlocking {
        // openapi.yaml: POST /items -> 201 Created
        // schema: Item (id as UUID, title, expirationDate, category, statusColor, storageLocation)
        val requestBody = ItemInput(
            title = "Aspirin",
            expirationDate = "2027-01-01",
            category = Category.MEDICINE,
            storageLocation = "First Aid Kit"
        )

        val response: ApiResponse<Item> = apiController.createItem(requestBody)

        assertEquals("Expected HTTP 201 Created", 201, response.status)
        assertTrue("Response should indicate success", response.isSuccessful)
        assertNull("ErrorResponse should be null for 201", response.error)

        val createdItem = response.data
        assertNotNull("Response body must contain created Item", createdItem)
        assertEquals("Aspirin", createdItem!!.title)
        assertEquals("2027-01-01", createdItem.expirationDate)
        assertEquals(Category.MEDICINE, createdItem.category)
        assertEquals("First Aid Kit", createdItem.storageLocation)

        // Verify UUID format
        assertNotNull("Item ID must be valid UUID", UUID.fromString(createdItem.id))

        // Verify calculated statusColor
        assertNotNull("Item statusColor must be populated", createdItem.statusColor)
    }

    @Test
    fun post_item_invalid_empty_title_returns_400_bad_request_with_ErrorResponse() = runBlocking {
        // openapi.yaml: POST /items -> 400 Bad Request with ErrorResponse
        val invalidRequest = ItemInput(
            title = "   ",
            expirationDate = "2027-01-01",
            category = Category.FOOD
        )

        val response: ApiResponse<Item> = apiController.createItem(invalidRequest)

        assertEquals("Expected HTTP 400 Bad Request", 400, response.status)
        assertFalse("Response should not indicate success", response.isSuccessful)
        assertNull("Data payload should be null for error response", response.data)

        val error = response.error
        assertNotNull("Body must contain ErrorResponse", error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/items", error.path)
        assertTrue("Error message must indicate empty title", error.message.contains("title", ignoreCase = true))
        assertNotNull("Timestamp must be present", error.timestamp)
    }

    @Test
    fun post_item_invalid_date_format_returns_400_bad_request_with_ErrorResponse() = runBlocking {
        // openapi.yaml: POST /items -> 400 Bad Request
        val invalidRequest = ItemInput(
            title = "Milk",
            expirationDate = "invalid-date",
            category = Category.FOOD
        )

        val response: ApiResponse<Item> = apiController.createItem(invalidRequest)

        assertEquals("Expected HTTP 400 Bad Request", 400, response.status)
        val error = response.error
        assertNotNull(error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/items", error.path)
        assertTrue("Error message must indicate date issue", error.message.contains("date", ignoreCase = true))
    }

    // ==========================================
    // 2. GET /api/v1/items Contract Tests
    // ==========================================

    @Test
    fun get_items_valid_returns_200_ok_with_chronologically_sorted_items() = runBlocking {
        // Seed items via API
        apiController.createItem(ItemInput("Far Future Item", "2028-01-01", Category.FOOD))
        apiController.createItem(ItemInput("Soon Expiring Item", "2026-11-10", Category.MEDICINE))
        apiController.createItem(ItemInput("Expired Item", "2026-11-01", Category.COSMETICS))

        val response: ApiResponse<List<Item>> = apiController.getItems(search = null, category = null)

        assertEquals("Expected HTTP 200 OK", 200, response.status)
        assertTrue(response.isSuccessful)
        assertNull(response.error)

        val items = response.data
        assertNotNull("Response body must contain items array", items)
        assertTrue("Must contain at least 3 items", items!!.size >= 3)

        // Strict chronological sort verification (soonest first)
        for (i in 0 until items.size - 1) {
            assertTrue(
                "Items must be sorted chronologically ascending",
                items[i].expirationDate <= items[i + 1].expirationDate
            )
        }
    }

    @Test
    fun get_items_search_query_case_insensitive_returns_200_ok_matching_items() = runBlocking {
        apiController.createItem(ItemInput("Aspirin 500mg", "2027-01-01", Category.MEDICINE))
        apiController.createItem(ItemInput("aspirin extra", "2027-02-01", Category.MEDICINE))
        apiController.createItem(ItemInput("Whole Milk", "2026-11-15", Category.FOOD))

        // Case-insensitive query "aSpIrIn"
        val response: ApiResponse<List<Item>> = apiController.getItems(search = "aSpIrIn", category = null)

        assertEquals("Expected HTTP 200 OK", 200, response.status)
        assertTrue(response.isSuccessful)
        assertNull(response.error)
        val items = response.data
        assertNotNull(items)
        assertEquals(2, items!!.size)
        assertTrue(items.all { it.title.contains("aspirin", ignoreCase = true) })
    }

    @Test
    fun get_items_category_filter_returns_200_ok_strictly_matching_enum_category() = runBlocking {
        apiController.createItem(ItemInput("Fresh Apples", "2026-12-01", Category.FOOD))
        apiController.createItem(ItemInput("Cough Syrup", "2027-05-01", Category.MEDICINE))
        apiController.createItem(ItemInput("Face Cream", "2027-08-01", Category.COSMETICS))
        apiController.createItem(ItemInput("Floor Cleaner", "2028-01-01", Category.CHEMICALS))

        val response: ApiResponse<List<Item>> = apiController.getItems(search = null, category = "Medicine")

        assertEquals("Expected HTTP 200 OK", 200, response.status)
        assertTrue(response.isSuccessful)
        val items = response.data
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals(Category.MEDICINE, items[0].category)
        assertEquals("Cough Syrup", items[0].title)
    }

    @Test
    fun get_items_search_nonexistent_returns_200_ok_with_empty_array() = runBlocking {
        apiController.createItem(ItemInput("Aspirin", "2027-01-01", Category.MEDICINE))

        val response: ApiResponse<List<Item>> = apiController.getItems(search = "NonExistentItemQuery123", category = null)

        assertEquals("Expected HTTP 200 OK for empty search result", 200, response.status)
        assertTrue(response.isSuccessful)
        assertNull(response.error)
        val items = response.data
        assertNotNull("Response data must be an empty list, not null", items)
        assertTrue("Response list must be empty", items!!.isEmpty())
    }

    @Test
    fun get_items_invalid_category_toys_returns_400_bad_request_with_ErrorResponse() = runBlocking {
        // openapi.yaml: GET /items?category=Toys -> 400 Bad Request
        // message: "Validation error: Invalid category 'Toys'. Allowed categories are Food, Medicine, Cosmetics, Chemicals."
        val response: ApiResponse<List<Item>> = apiController.getItems(search = null, category = "Toys")

        assertEquals("Expected HTTP 400 Bad Request for invalid category", 400, response.status)
        assertFalse(response.isSuccessful)
        assertNull("Data should be null on 400 error", response.data)

        val error = response.error
        assertNotNull("ErrorResponse body must be present", error)
        assertEquals(400, error!!.status)
        assertEquals("Bad Request", error.error)
        assertEquals("/api/v1/items", error.path)
        assertTrue(
            "Message must mention invalid category and allowed categories",
            error.message.contains("Toys") && error.message.contains("Food, Medicine, Cosmetics, Chemicals")
        )
    }

    // ==========================================
    // 3. DELETE /api/v1/items/{id} Contract Tests
    // ==========================================

    @Test
    fun delete_item_existing_id_returns_204_no_content_and_permanently_removes_item() = runBlocking {
        // openapi.yaml: DELETE /items/{id} -> 204 No Content
        val createResponse = apiController.createItem(
            ItemInput("Item To Delete", "2027-01-01", Category.FOOD)
        )
        val createdItem = createResponse.data
        assertNotNull("Created item must not be null", createdItem)
        val itemId = createdItem!!.id

        val deleteResponse: ApiResponse<Unit> = apiController.deleteItem(itemId)

        assertEquals("Expected HTTP 204 No Content", 204, deleteResponse.status)
        assertTrue("Response should indicate success", deleteResponse.isSuccessful)
        assertNull("Data should be null for 204 No Content", deleteResponse.data)
        assertNull("Error should be null for successful deletion", deleteResponse.error)

        // Verify permanently removed: GET /api/v1/items does not contain the item
        val getResponse = apiController.getItems()
        val currentItems = getResponse.data ?: emptyList()
        assertFalse("Deleted item must not exist in items list", currentItems.any { it.id == itemId })
    }

    @Test
    fun delete_item_non_existent_id_returns_404_not_found_with_ErrorResponse() = runBlocking {
        // openapi.yaml: DELETE /items/{id} -> 404 Not Found with ErrorResponse
        val nonExistentId = UUID.randomUUID().toString()

        val deleteResponse: ApiResponse<Unit> = apiController.deleteItem(nonExistentId)

        assertEquals("Expected HTTP 404 Not Found", 404, deleteResponse.status)
        assertFalse("Response should indicate failure", deleteResponse.isSuccessful)
        assertNull("Data payload should be null for error response", deleteResponse.data)

        val error = deleteResponse.error
        assertNotNull("Body must contain ErrorResponse", error)
        assertEquals(404, error!!.status)
        assertEquals("Not Found", error.error)
        assertEquals("/api/v1/items/$nonExistentId", error.path)
        assertTrue(
            "Error message must contain item ID or indicate not found",
            error.message.contains(nonExistentId) || error.message.contains("not found", ignoreCase = true)
        )
        assertNotNull("Timestamp must be present", error.timestamp)
    }
}
