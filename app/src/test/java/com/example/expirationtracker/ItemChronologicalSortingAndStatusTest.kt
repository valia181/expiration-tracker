package com.example.expirationtracker

import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.StatusColor
import com.example.expirationtracker.domain.usecase.GetSortedItemsWithStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit Test Suite for TSK-004: Chronological List and Color Status Calculation.
 *
 * Traceability:
 * - REQ-F-004: Display all saved items in a unified chronological list sorted by expiration date (soonest first).
 * - REQ-F-005: Dynamically assign and display status color:
 *     - Red: Expiration date has passed (Date < Today).
 *     - Orange: Expiration date falls within the configured global notification lead time window (Today <= Date <= Today + Lead Time).
 *     - Green: Expiration date is strictly beyond the configured lead time window (Date > Today + Lead Time).
 * - REQ-NF-005: Status color categorization.
 * - AC-F-004, AC-F-005.
 */
class ItemChronologicalSortingAndStatusTest {

    private val referenceToday: LocalDate = LocalDate.of(2026, 11, 5)
    private val globalLeadTimeDays: Int = 7

    private lateinit var statusCalculator: StatusColorCalculator
    private lateinit var getSortedItemsUseCase: GetSortedItemsWithStatusUseCase

    @Before
    fun setUp() {
        statusCalculator = StatusColorCalculator()
        getSortedItemsUseCase = GetSortedItemsWithStatusUseCase(statusCalculator)
    }

    // ==========================================
    // 1. StatusColor Calculation & Boundary Tests (REQ-F-005, AC-F-005)
    // ==========================================

    @Test
    fun calculateStatus_returns_RED_when_item_expired_in_past() {
        // Given expiration date is yesterday (Date < Today)
        val expiredDate = referenceToday.minusDays(1)
        val status = statusCalculator.calculateStatus(
            expirationDate = expiredDate,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals("Date in the past must be RED (Expired)", StatusColor.RED, status)
    }

    @Test
    fun calculateStatus_returns_RED_when_item_expired_long_ago() {
        // Given expiration date is 60 days in the past
        val longAgo = referenceToday.minusDays(60)
        val status = statusCalculator.calculateStatus(
            expirationDate = longAgo,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals("Date long in the past must be RED (Expired)", StatusColor.RED, status)
    }

    @Test
    fun calculateStatus_returns_ORANGE_when_item_expires_exactly_today() {
        // Boundary: Date == Today (falls within lead time window)
        val status = statusCalculator.calculateStatus(
            expirationDate = referenceToday,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals("Item expiring exactly today must be ORANGE (Warning)", StatusColor.ORANGE, status)
    }

    @Test
    fun calculateStatus_returns_ORANGE_when_item_expires_within_lead_time_window() {
        // Given expiration date is 3 days from today (within 7-day window)
        val withinLeadTime = referenceToday.plusDays(3)
        val status = statusCalculator.calculateStatus(
            expirationDate = withinLeadTime,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals("Item expiring within lead time window must be ORANGE (Warning)", StatusColor.ORANGE, status)
    }

    @Test
    fun calculateStatus_returns_ORANGE_when_item_expires_exactly_on_lead_time_boundary() {
        // Boundary: Date == Today + LeadTime (e.g. 2026-11-05 + 7 days = 2026-11-12)
        val exactBoundary = referenceToday.plusDays(globalLeadTimeDays.toLong())
        val status = statusCalculator.calculateStatus(
            expirationDate = exactBoundary,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals(
            "Item expiring exactly on the lead time boundary must be ORANGE (Warning)",
            StatusColor.ORANGE,
            status
        )
    }

    @Test
    fun calculateStatus_returns_GREEN_when_item_expires_one_day_beyond_lead_time_boundary() {
        // Boundary: Date == Today + LeadTime + 1 day (strictly beyond window)
        val justBeyondBoundary = referenceToday.plusDays(globalLeadTimeDays.toLong() + 1)
        val status = statusCalculator.calculateStatus(
            expirationDate = justBeyondBoundary,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals(
            "Item expiring 1 day beyond lead time must be GREEN (Safe)",
            StatusColor.GREEN,
            status
        )
    }

    @Test
    fun calculateStatus_returns_GREEN_when_item_expires_far_in_future() {
        // Given expiration date is far in the future (e.g. 1 year ahead)
        val farFuture = referenceToday.plusYears(1)
        val status = statusCalculator.calculateStatus(
            expirationDate = farFuture,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals("Item expiring far in the future must be GREEN (Safe)", StatusColor.GREEN, status)
    }

    @Test
    fun calculateStatus_adjusts_dynamically_when_lead_time_changes() {
        // REQ-F-008 & AC-F-008: Global lead time can be 1, 3, 7, 14 days
        val testDate = referenceToday.plusDays(5)

        // For leadTime = 3: 5 days is strictly beyond 3 -> GREEN
        val status3Days = statusCalculator.calculateStatus(testDate, referenceToday, leadTimeDays = 3)
        assertEquals(StatusColor.GREEN, status3Days)

        // For leadTime = 7: 5 days is within 7 -> ORANGE
        val status7Days = statusCalculator.calculateStatus(testDate, referenceToday, leadTimeDays = 7)
        assertEquals(StatusColor.ORANGE, status7Days)

        // For leadTime = 14: 5 days is within 14 -> ORANGE
        val status14Days = statusCalculator.calculateStatus(testDate, referenceToday, leadTimeDays = 14)
        assertEquals(StatusColor.ORANGE, status14Days)
    }

    // ==========================================
    // 2. Chronological Sorting & Integration (REQ-F-004, AC-F-004)
    // ==========================================

    @Test
    fun getSortedItems_sorts_items_in_strict_ascending_chronological_order_with_assigned_status() {
        // Given unsorted items with mixed expiration dates
        val itemExpired = Item(
            id = "1",
            title = "Old Milk",
            expirationDate = "2026-11-01",
            category = Category.FOOD
        )
        val itemToday = Item(
            id = "2",
            title = "Bread Today",
            expirationDate = "2026-11-05",
            category = Category.FOOD
        )
        val itemFarFuture = Item(
            id = "3",
            title = "Canned Tuna",
            expirationDate = "2027-12-31",
            category = Category.FOOD
        )
        val itemWarning = Item(
            id = "4",
            title = "Yogurt Warning",
            expirationDate = "2026-11-09",
            category = Category.FOOD
        )

        val unsortedList = listOf(itemFarFuture, itemExpired, itemWarning, itemToday)

        // When usecase executes
        val sortedList = getSortedItemsUseCase.execute(
            items = unsortedList,
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )

        // Then items are strictly sorted ascending by date (soonest first)
        assertEquals(4, sortedList.size)
        assertEquals("Old Milk", sortedList[0].title)
        assertEquals("2026-11-01", sortedList[0].expirationDate)
        assertEquals(StatusColor.RED, sortedList[0].statusColor)

        assertEquals("Bread Today", sortedList[1].title)
        assertEquals("2026-11-05", sortedList[1].expirationDate)
        assertEquals(StatusColor.ORANGE, sortedList[1].statusColor)

        assertEquals("Yogurt Warning", sortedList[2].title)
        assertEquals("2026-11-09", sortedList[2].expirationDate)
        assertEquals(StatusColor.ORANGE, sortedList[2].statusColor)

        assertEquals("Canned Tuna", sortedList[3].title)
        assertEquals("2027-12-31", sortedList[3].expirationDate)
        assertEquals(StatusColor.GREEN, sortedList[3].statusColor)
    }

    @Test
    fun getSortedItems_handles_items_with_identical_expiration_dates() {
        val itemA = Item(id = "1", title = "Aspirin", expirationDate = "2026-11-10", category = Category.MEDICINE)
        val itemB = Item(id = "2", title = "Bandages", expirationDate = "2026-11-10", category = Category.MEDICINE)

        val result = getSortedItemsUseCase.execute(
            items = listOf(itemA, itemB),
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )

        assertEquals(2, result.size)
        assertEquals("2026-11-10", result[0].expirationDate)
        assertEquals("2026-11-10", result[1].expirationDate)
        assertEquals(StatusColor.ORANGE, result[0].statusColor)
        assertEquals(StatusColor.ORANGE, result[1].statusColor)
    }

    @Test
    fun getSortedItems_returns_empty_list_when_input_is_empty() {
        val result = getSortedItemsUseCase.execute(
            items = emptyList(),
            currentDate = referenceToday,
            leadTimeDays = globalLeadTimeDays
        )
        assertEquals(emptyList<Item>(), result)
    }
}
