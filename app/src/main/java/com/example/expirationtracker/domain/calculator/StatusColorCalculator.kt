package com.example.expirationtracker.domain.calculator

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.domain.model.StatusColor
import java.time.LocalDate

/**
 * Encapsulates the core 3-level dynamic color status calculation algorithm per REQ-F-005 and AC-F-005:
 * - **Red (Expired):** The expiration date is strictly before the current date (`Date < Today`).
 * - **Orange (Warning):** The expiration date falls within the global notification lead time window (`Today <= Date <= Today + LeadTime`).
 * - **Green (Safe):** The expiration date is strictly beyond the global notification lead time window (`Date > Today + LeadTime`).
 */
class StatusColorCalculator {

    companion object {
        private const val TAG = "StatusColorCalculator"
    }

    /**
     * Computes the [StatusColor] for an item based on its expiration date, baseline date, and configured lead time window.
     *
     * @param expirationDate The calendar expiration date of the item.
     * @param currentDate The baseline date representing "today" (supports testing with arbitrary dates).
     * @param leadTimeDays The global notification lead time in days (must be 1, 3, 7, or 14).
     * @return The computed [StatusColor] ([StatusColor.RED], [StatusColor.ORANGE], or [StatusColor.GREEN]).
     */
    fun calculateStatus(
        expirationDate: LocalDate,
        currentDate: LocalDate,
        leadTimeDays: Int
    ): StatusColor {
        val boundaryDate = currentDate.plusDays(leadTimeDays.toLong())

        val status = when {
            expirationDate.isBefore(currentDate) -> StatusColor.RED
            !expirationDate.isAfter(boundaryDate) -> StatusColor.ORANGE
            else -> StatusColor.GREEN
        }

        SystemLogger.debug(
            TAG,
            "Calculated status for date=$expirationDate (today=$currentDate, leadTime=$leadTimeDays): $status"
        )
        return status
    }
}
