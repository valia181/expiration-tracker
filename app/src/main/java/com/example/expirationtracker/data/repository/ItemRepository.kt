package com.example.expirationtracker.data.repository

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.entity.ItemEntity
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.model.StatusColor
import com.example.expirationtracker.domain.validation.ItemValidator
import com.example.expirationtracker.domain.validation.ValidationResult

/**
 * Repository interface defining item data access operations.
 * Traceability: REQ-NF-007 (Architecture separation).
 */
interface ItemRepository {
    fun createItem(input: ItemInput): Result<Item>
    fun getAllItems(): List<Item>
    fun deleteItem(id: String): Boolean
}

/**
 * Concrete ItemRepository implementation delegating to SQLite DAO and validating via ItemValidator.
 */
class ItemRepositoryImpl(
    private val itemDao: ItemDao,
    private val validator: ItemValidator = ItemValidator()
) : ItemRepository {

    override fun createItem(input: ItemInput): Result<Item> {
        SystemLogger.info("ItemRepository", "Attempting to create item with title: '${input.title}'")

        val validationResult: ValidationResult = validator.validate(input)
        if (!validationResult.isValid) {
            val errorMsg = validationResult.errors.joinToString("; ")
            SystemLogger.warn("ItemRepository", "Item creation aborted: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        val domainItem = Item(
            title = input.title.trim(),
            expirationDate = input.expirationDate.trim(),
            category = input.category,
            storageLocation = input.storageLocation?.trim()?.ifBlank { null },
            statusColor = StatusColor.GREEN
        )

        val entity = ItemEntity(
            id = domainItem.id,
            title = domainItem.title,
            expirationDate = domainItem.expirationDate,
            category = domainItem.category.name,
            storageLocation = domainItem.storageLocation,
            statusColor = domainItem.statusColor.name
        )

        try {
            itemDao.insert(entity)
            SystemLogger.info("ItemRepository", "Item successfully stored with ID: ${domainItem.id}")
            return Result.success(domainItem)
        } catch (e: Exception) {
            SystemLogger.error("ItemRepository", "Failed to store item in database", e)
            return Result.failure(e)
        }
    }

    override fun getAllItems(): List<Item> {
        return itemDao.getAllSortedByExpirationDate().map { entity ->
            Item(
                id = entity.id,
                title = entity.title,
                expirationDate = entity.expirationDate,
                category = Category.valueOf(entity.category),
                storageLocation = entity.storageLocation,
                statusColor = StatusColor.valueOf(entity.statusColor)
            )
        }
    }

    override fun deleteItem(id: String): Boolean {
        SystemLogger.info("ItemRepository", "Deleting item id: $id")
        return itemDao.deleteById(id) > 0
    }
}
