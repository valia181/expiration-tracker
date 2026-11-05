package com.example.expirationtracker.domain.model

/**
 * 3-level dynamic color status per SRS Section 3 and REQ-F-005.
 * - Red: Expired (Date < Today)
 * - Orange: Approaching expiration within global notification lead time window
 * - Green: Safe (Date > Today + Lead Time)
 */
enum class StatusColor(val displayName: String) {
    RED("Red"),
    ORANGE("Orange"),
    GREEN("Green");

    companion object {
        fun fromDisplayName(name: String): StatusColor? =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}
