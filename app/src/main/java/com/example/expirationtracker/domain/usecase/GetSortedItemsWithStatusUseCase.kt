package com.example.expirationtracker.domain.usecase

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.Item
import java.time.LocalDate

/**
 * Use case responsible for processing a raw list of items by sorting them chronologically (soonest expiring first)
 * and dynamically computing their status colors (Red, Orange, Green) based on expiration dates and global lead time settings.
 *
 * Traceability: REQ-F-004, REQ-F-005, AC-F-004, AC-F-005.
 */
class GetSortedItemsWithStatusUseCase(
    private val statusCalculator: StatusColorCalculator = StatusColorCalculator()
) {

    companion object {
        private const val TAG = "GetSortedItemsWithStatusUseCase"
    }

    /**
     * Executes chronological sorting and status color computation on the provided item collection.
     *
     * @param items The raw list of [Item] instances to process.
     * @param currentDate The baseline date used for status comparisons (defaults to [LocalDate.now]).
     * @param leadTimeDays The global notification lead time in days (must be 1, 3, 7, or 14).
     * @return A new list of [Item] objects sorted in ascending order by expiration date, with computed [Item.statusColor] assigned.
     */
    fun execute(
        items: List<Item>,
        currentDate: LocalDate = LocalDate.now(),
        leadTimeDays: Int = 7
    ): List<Item> {
        SystemLogger.info(TAG, "Sorting and assigning status colors for ${items.size} items.")

        val sortedItems = items
            .sortedBy { it.expirationDate }
            .map { item ->
                val date = try {
                    LocalDate.parse(item.expirationDate)
                } catch (e: Exception) {
                    SystemLogger.warn(TAG, "Failed to parse item expiration date '${item.expirationDate}', defaulting to currentDate")
                    currentDate
                }
                val status = statusCalculator.calculateStatus(date, currentDate, leadTimeDays)
                item.copy(statusColor = status)
            }

        SystemLogger.info(TAG, "Successfully processed ${sortedItems.size} items.")
        return sortedItems
    }
}
