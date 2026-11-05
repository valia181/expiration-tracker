package com.example.expirationtracker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expirationtracker.data.api.ItemApiController
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper
import com.example.expirationtracker.data.repository.ItemRepositoryImpl
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ItemInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Dedicated Regression Test Suite for Category Filtering Bug (TSK-008/TSK-009).
 *
 * Traceability:
 * - REQ-F-007: Category filter controls (Food, Medicine, Cosmetics, Chemicals, All).
 * - AC-F-007: Only items belonging to the selected category are displayed. "All" returns all items.
 * - openapi.yaml: GET /api/v1/items?category={category}
 */
@RunWith(AndroidJUnit4::class)
class CategoryFilterBugTest {

    private lateinit var dbHelper: ExpirationTrackerDbHelper
    private lateinit var itemDao: ItemDao
    private lateinit var repository: ItemRepositoryImpl
    private lateinit var apiController: ItemApiController

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        dbHelper = ExpirationTrackerDbHelper(context)
        itemDao = ItemDao(dbHelper)
        itemDao.deleteAll()
        repository = ItemRepositoryImpl(itemDao)
        apiController = ItemApiController(repository = repository)
    }

    @Test
    fun category_filter_returns_matching_items_for_exact_and_case_variations() = runBlocking {
        // Populate SQLite / Repository with items across all predefined categories
        apiController.createItem(ItemInput("Fresh Bread", "2026-11-20", Category.FOOD))
        apiController.createItem(ItemInput("Aspirin 500mg", "2027-01-01", Category.MEDICINE))
        apiController.createItem(ItemInput("Face Moisturizer", "2027-06-01", Category.COSMETICS))
        apiController.createItem(ItemInput("Bleach Cleaner", "2028-01-01", Category.CHEMICALS))

        // Verify total items = 4 when category is omitted or 'All'
        val allResponse = apiController.getItems(search = null, category = null)
        assertEquals(200, allResponse.status)
        assertEquals(4, allResponse.data?.size)

        val allExplicitResponse = apiController.getItems(search = null, category = "All")
        assertEquals(200, allExplicitResponse.status)
        assertEquals(4, allExplicitResponse.data?.size)

        // Query GET /api/v1/items?category=Food
        val foodResponse = apiController.getItems(search = null, category = "Food")
        assertEquals(200, foodResponse.status)
        assertNotNull("Data must not be null", foodResponse.data)
        assertEquals(
            "Filtering by category 'Food' must return matching items instead of empty list",
            1,
            foodResponse.data?.size
        )
        assertEquals("Fresh Bread", foodResponse.data?.get(0)?.title)

        // Test case variations: 'food' and 'FOOD'
        val foodLowercaseResponse = apiController.getItems(search = null, category = "food")
        assertEquals(200, foodLowercaseResponse.status)
        assertEquals(1, foodLowercaseResponse.data?.size)

        val foodUppercaseResponse = apiController.getItems(search = null, category = "FOOD")
        assertEquals(200, foodUppercaseResponse.status)
        assertEquals(1, foodUppercaseResponse.data?.size)

        // Test other categories: Medicine, Cosmetics, Chemicals
        val medicineResponse = apiController.getItems(search = null, category = "Medicine")
        assertEquals(1, medicineResponse.data?.size)
        assertEquals("Aspirin 500mg", medicineResponse.data?.get(0)?.title)

        val cosmeticsResponse = apiController.getItems(search = null, category = "Cosmetics")
        assertEquals(1, cosmeticsResponse.data?.size)

        val chemicalsResponse = apiController.getItems(search = null, category = "Chemicals")
        assertEquals(1, chemicalsResponse.data?.size)
    }
}
