package com.example.expirationtracker.domain.model

/**
 * Domain model representing database backup payload (settings + items list)
 * matching spec/openapi.yaml BackupData schema.
 * Traceability: REQ-F-009, REQ-F-010.
 */
data class BackupData(
    val settings: Settings,
    val items: List<Item>
)
