package com.example.expirationtracker

import com.example.expirationtracker.data.api.ItemApiController
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.entity.ItemEntity
import com.example.expirationtracker.data.repository.ItemRepositoryImpl
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ItemInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Dedicated Regression Test Suite for Category Filtering Bug (TSK-008/TSK-009).
 *
 * Traceability:
 * - REQ-F-007: Category filter controls (Food, Medicine, Cosmetics, Chemicals, All).
 * - AC-F-007: Only items belonging to the selected category are displayed. "All" returns all items.
 * - openapi.yaml: GET /api/v1/items?category={category}
 */
class CategoryFilterBugTest {

    @Test
    fun category_filter_returns_matching_items_for_exact_and_case_variations() = runBlocking {
        val storedEntities = mutableListOf<ItemEntity>()

        // Fake DAO simulating ItemDao searchAndFilterItems behavior with SQL case sensitive match against stored entity.category ("FOOD")
        val fakeDao = object : ItemDao() {
            override fun insert(item: ItemEntity): Long {
                storedEntities.add(item)
                return 1L
            }

            override fun searchAndFilterItems(search: String?, category: String?): List<ItemEntity> {
                var list = storedEntities.toList()
                if (!search.isNullOrBlank()) {
                    list = list.filter { it.title.contains(search, ignoreCase = true) }
                }
                if (!category.isNullOrBlank()) {
                    // ItemDao executes SQL 'WHERE category = ?' against stored entity.category ("FOOD")
                    list = list.filter { it.category == category }
                }
                return list
            }

            override fun getAllSortedByExpirationDate(): List<ItemEntity> {
                return storedEntities.sortedBy { it.expirationDate }
            }
        }

        val repository = ItemRepositoryImpl(fakeDao)
        val apiController = ItemApiController(repository = repository)

        // Populate items across all predefined categories
        apiController.createItem(ItemInput("Fresh Bread", "2026-11-20", Category.FOOD))
        apiController.createItem(ItemInput("Aspirin 500mg", "2027-01-01", Category.MEDICINE))
        apiController.createItem(ItemInput("Face Moisturizer", "2027-06-01", Category.COSMETICS))
        apiController.createItem(ItemInput("Bleach Cleaner", "2028-01-01", Category.CHEMICALS))

        // 1. Verify total items = 4 when category is omitted
        val allResponse = apiController.getItems(search = null, category = null)
        assertEquals(200, allResponse.status)
        assertEquals(4, allResponse.data?.size)

        // 2. Query GET /api/v1/items?category=Food
        val foodResponse = apiController.getItems(search = null, category = "Food")
        assertEquals(200, foodResponse.status)
        assertNotNull("Data must not be null", foodResponse.data)
        assertEquals(
            "Filtering by category 'Food' must return matching items instead of empty list",
            1,
            foodResponse.data?.size
        )
        assertEquals("Fresh Bread", foodResponse.data?.get(0)?.title)

        // 3. Test case variations: 'food' and 'FOOD'
        val foodLowercaseResponse = apiController.getItems(search = null, category = "food")
        assertEquals(200, foodLowercaseResponse.status)
        assertEquals(1, foodLowercaseResponse.data?.size)

        val foodUppercaseResponse = apiController.getItems(search = null, category = "FOOD")
        assertEquals(200, foodUppercaseResponse.status)
        assertEquals(1, foodUppercaseResponse.data?.size)

        // 4. Test other categories: Medicine, Cosmetics, Chemicals
        val medicineResponse = apiController.getItems(search = null, category = "Medicine")
        assertEquals(1, medicineResponse.data?.size)
        assertEquals("Aspirin 500mg", medicineResponse.data?.get(0)?.title)

        val cosmeticsResponse = apiController.getItems(search = null, category = "Cosmetics")
        assertEquals(1, cosmeticsResponse.data?.size)

        val chemicalsResponse = apiController.getItems(search = null, category = "Chemicals")
        assertEquals(1, chemicalsResponse.data?.size)
    }
}
