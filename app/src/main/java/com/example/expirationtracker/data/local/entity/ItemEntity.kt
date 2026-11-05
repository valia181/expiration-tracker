package com.example.expirationtracker.data.local.entity

/**
 * Data entity representing an item record in SQLite (TABLE items).
 */
data class ItemEntity(
    val id: String,
    val title: String,
    val expirationDate: String,
    val category: String,
    val storageLocation: String?,
    val statusColor: String
)
