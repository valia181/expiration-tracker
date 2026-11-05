package com.example.expirationtracker.domain.model

/**
 * Input model for manual item creation (REQ-F-001, OpenAPI ItemInput schema).
 */
data class ItemInput(
    val title: String,
    val expirationDate: String,
    val category: Category,
    val storageLocation: String? = null
)
