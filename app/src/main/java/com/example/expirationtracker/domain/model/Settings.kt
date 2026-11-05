package com.example.expirationtracker.domain.model

/**
 * Settings domain model for global notification lead time.
 * Allowed lead time values: 1, 3, 7, 14 days per openapi.yaml and REQ-F-008.
 */
data class Settings(
    val notificationLeadTime: Int = 7
) {
    companion object {
        val ALLOWED_LEAD_TIMES = listOf(1, 3, 7, 14)

        fun isValidLeadTime(leadTime: Int): Boolean {
            return ALLOWED_LEAD_TIMES.contains(leadTime)
        }
    }
}
