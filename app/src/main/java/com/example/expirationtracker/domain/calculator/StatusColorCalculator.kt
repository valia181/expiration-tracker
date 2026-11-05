package com.example.expirationtracker.domain.calculator

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.domain.model.StatusColor
import java.time.LocalDate

/**
 * Calculates dynamic status color per REQ-F-005 and AC-F-005:
 * - Red (Expired): Date < Today
 * - Orange (Warning): Today <= Date <= Today + LeadTime
 * - Green (Safe): Date > Today + LeadTime
 */
class StatusColorCalculator {

    companion object {
        private const val TAG = "StatusColorCalculator"
    }

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
