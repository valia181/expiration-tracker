package com.example.expirationtracker.domain.model

import java.util.UUID

/**
 * Domain Item entity representing a tracked household item.
 * Traceability: REQ-F-001, REQ-F-005, OpenAPI Item schema.
 */
data class Item(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val expirationDate: String,
    val category: Category,
    val storageLocation: String? = null,
    val statusColor: StatusColor = StatusColor.GREEN
)
