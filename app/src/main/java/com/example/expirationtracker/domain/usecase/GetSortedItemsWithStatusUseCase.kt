package com.example.expirationtracker.domain.usecase

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.Item
import java.time.LocalDate

/**
 * UseCase to retrieve items sorted chronologically (soonest first)
 * with dynamically assigned status colors.
 * Traceability: REQ-F-004, REQ-F-005, AC-F-004, AC-F-005.
 */
class GetSortedItemsWithStatusUseCase(
    private val statusCalculator: StatusColorCalculator = StatusColorCalculator()
) {

    companion object {
        private const val TAG = "GetSortedItemsWithStatusUseCase"
    }

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
