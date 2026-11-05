package com.example.expirationtracker.domain.model

/**
 * 3-level dynamic color status per SRS Section 3 and REQ-F-005.
 * - Red: Expired (Date < Today)
 * - Orange: Approaching expiration within global notification lead time window
 * - Green: Safe (Date > Today + Lead Time)
 */
enum class StatusColor {
    RED,
    ORANGE,
    GREEN
}
