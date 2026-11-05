package com.example.expirationtracker

import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.StatusColor
import com.example.expirationtracker.domain.usecase.GetSortedItemsWithStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/**
 * BDD Scenarios (AC-F-004, AC-F-005) & OpenAPI GET /items Contract Verification Test.
 *
 * Traceability:
 * - REQ-F-004 & AC-F-004: Chronological display (soonest first).
 * - REQ-F-005 & AC-F-005: Dynamic color status assignment (Red, Orange, Green).
 * - openapi.yaml: GET /items response schema (array of Item objects, sorted chronologically).
 */
class ItemListContractAndBddTest {

    private val referenceToday: LocalDate = LocalDate.of(2026, 11, 5)
    private val globalLeadTimeDays: Int = 7

    private lateinit var getSortedItemsUseCase: GetSortedItemsWithStatusUseCase

    @Before
    fun setUp() {
        val calculator = StatusColorCalculator()
        getSortedItemsUseCase = GetSortedItemsWithStatusUseCase(calculator)
    }

    // ==========================================
    // 1. BDD Scenarios (AC-F-004 & AC-F-005)
    // ==========================================

    @Test
    fun bdd_scenario_AC_F_004_chronological_display_ascending_order() {
        // Given multiple items with varying expiration dates exist in the database
        val itemA = Item(id = "1", title = "Pears", expirationDate = "2026-11-20", category = Category.FOOD)
        val itemB = Item(id = "2", title = "Yogurt", expirationDate = "2026-11-06", category = Category.FOOD)
        val itemC = Item(id = "3", title = "Expired Juice", expirationDate = "2026-11-01", category = Category.FOOD)
        val itemD = Item(id = "4", title = "Salt", expirationDate = "2028-05-10", category = Category.FOOD)
        val dbItems = listOf(itemA, itemB, itemC, itemD)

        // When the main screen loads (use case executes)
        val renderedItems = getSortedItemsUseCase.execute(
            items = dbItems,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )

        // Then items are rendered in a single unified list sorted strictly in ascending order by expiration date (soonest first)
        assertEquals(4, renderedItems.size)
        assertEquals("Expired Juice", renderedItems[0].title)
        assertEquals("2026-11-01", renderedItems[0].expirationDate)

        assertEquals("Yogurt", renderedItems[1].title)
        assertEquals("2026-11-06", renderedItems[1].expirationDate)

        assertEquals("Pears", renderedItems[2].title)
        assertEquals("2026-11-20", renderedItems[2].expirationDate)

        assertEquals("Salt", renderedItems[3].title)
        assertEquals("2028-05-10", renderedItems[3].expirationDate)
    }

    @Test
    fun bdd_scenario_AC_F_005_dynamic_color_status_rules() {
        // Given the items are displayed in the list:
        // - An item whose expiration date is before the current date displays a Red indicator.
        // - An item whose expiration date falls within the configured global notification lead time displays an Orange indicator.
        // - An item whose expiration date is strictly beyond the configured Notification Lead Time window displays a Green indicator.
        val expiredItem = Item(id = "1", title = "Expired Cereal", expirationDate = "2026-11-04", category = Category.FOOD)
        val todayItem = Item(id = "2", title = "Bread Expiring Today", expirationDate = "2026-11-05", category = Category.FOOD)
        val warningItem = Item(id = "3", title = "Milk Within Lead Time", expirationDate = "2026-11-08", category = Category.FOOD)
        val leadTimeBoundaryItem = Item(id = "4", title = "Cheese on Boundary", expirationDate = "2026-11-12", category = Category.FOOD)
        val safeItem = Item(id = "5", title = "Canned Corn Safe", expirationDate = "2026-11-13", category = Category.FOOD)

        val items = listOf(expiredItem, todayItem, warningItem, leadTimeBoundaryItem, safeItem)

        val processed = getSortedItemsUseCase.execute(
            items = items,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays // 7 days (boundary is 2026-11-12)
        )

        assertEquals(StatusColor.RED, processed[0].statusColor) // 2026-11-04 < 2026-11-05
        assertEquals(StatusColor.ORANGE, processed[1].statusColor) // 2026-11-05 == Today
        assertEquals(StatusColor.ORANGE, processed[2].statusColor) // 2026-11-08 in (Today..Today+7)
        assertEquals(StatusColor.ORANGE, processed[3].statusColor) // 2026-11-12 == Today+7
        assertEquals(StatusColor.GREEN, processed[4].statusColor) // 2026-11-13 > Today+7
    }

    // ==========================================
    // 2. OpenAPI GET /items Contract Verification
    // ==========================================

    @Test
    fun openapi_contract_get_items_returns_schema_compliant_array_sorted_chronologically() {
        // openapi.yaml:
        // GET /items -> 200 OK: array of components/schemas/Item
        // Item schema requires: id (UUID), title, expirationDate (date), category (enum), statusColor (enum: Red, Orange, Green)
        val mockItems = listOf(
            Item(id = UUID.randomUUID().toString(), title = "Bleach", expirationDate = "2027-01-01", category = Category.CHEMICALS),
            Item(id = UUID.randomUUID().toString(), title = "Aspirin", expirationDate = "2026-11-01", category = Category.MEDICINE)
        )

        val responseItems = getSortedItemsUseCase.execute(
            items = mockItems,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )

        // 1. Response is an array
        assertNotNull(responseItems)
        assertEquals(2, responseItems.size)

        // 2. Strict chronological order (soonest first)
        assertEquals("Aspirin", responseItems[0].title)
        assertEquals("Bleach", responseItems[1].title)

        // 3. Every item satisfies openapi.yaml Item schema properties
        for (item in responseItems) {
            // id: string (UUID format)
            assertNotNull(UUID.fromString(item.id))
            // title: non-empty string
            assertTrue(item.title.isNotBlank())
            // expirationDate: YYYY-MM-DD
            assertTrue(item.expirationDate.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
            // category: one of [Food, Medicine, Cosmetics, Chemicals]
            assertTrue(listOf("Food", "Medicine", "Cosmetics", "Chemicals").contains(item.category.displayName))
            // statusColor: one of [Red, Orange, Green]
            assertTrue(listOf("Red", "Orange", "Green").contains(item.statusColor.displayName))
        }
    }

    // ==========================================
    // 3. Edge and Boundary Tests
    // ==========================================

    @Test
    fun edge_case_empty_database_returns_empty_list() {
        // When database contains no items
        val result = getSortedItemsUseCase.execute(
            items = emptyList(),
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )

        // Then returns empty list without error
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun edge_case_multiple_items_with_exact_same_expiration_date() {
        // Multiple items sharing identical expiration date
        val item1 = Item(id = "1", title = "Vitamin D", expirationDate = "2026-11-07", category = Category.MEDICINE)
        val item2 = Item(id = "2", title = "Milk", expirationDate = "2026-11-07", category = Category.FOOD)
        val item3 = Item(id = "3", title = "Face Cream", expirationDate = "2026-11-07", category = Category.COSMETICS)

        val result = getSortedItemsUseCase.execute(
            items = listOf(item1, item2, item3),
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )

        assertEquals(3, result.size)
        // All retain valid ordering and identical dynamic status (Orange for 2026-11-07)
        for (item in result) {
            assertEquals("2026-11-07", item.expirationDate)
            assertEquals(StatusColor.ORANGE, item.statusColor)
        }
    }

    @Test
    fun edge_case_item_expiring_exactly_today_vs_tomorrow() {
        // Today = 2026-11-05
        val itemToday = Item(id = "today", title = "Sushi", expirationDate = "2026-11-05", category = Category.FOOD)
        val itemTomorrow = Item(id = "tomorrow", title = "Salad", expirationDate = "2026-11-06", category = Category.FOOD)

        val result = getSortedItemsUseCase.execute(
            items = listOf(itemTomorrow, itemToday),
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )

        // Soonest first: today before tomorrow
        assertEquals("Sushi", result[0].title)
        assertEquals("Salad", result[1].title)

        // Both in warning (Orange), but ordered sequentially
        assertEquals(StatusColor.ORANGE, result[0].statusColor)
        assertEquals(StatusColor.ORANGE, result[1].statusColor)
    }
}
