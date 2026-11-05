package com.example.expirationtracker.data.repository

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.mapper.ItemMapper
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

    companion object {
        private const val TAG = "ItemRepository"
    }

    override fun createItem(input: ItemInput): Result<Item> {
        SystemLogger.info(TAG, "Attempting to create item with title: '${input.title}'")

        val validationResult: ValidationResult = validator.validate(input)
        if (!validationResult.isValid) {
            val errorMsg = validationResult.errors.joinToString("; ")
            SystemLogger.warn(TAG, "Item creation aborted: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        val domainItem = Item(
            title = input.title.trim(),
            expirationDate = input.expirationDate.trim(),
            category = input.category,
            storageLocation = input.storageLocation?.trim()?.ifBlank { null },
            statusColor = StatusColor.GREEN
        )

        val entity = ItemMapper.toEntity(domainItem)

        return try {
            itemDao.insert(entity)
            SystemLogger.info(TAG, "Item successfully stored with ID: ${domainItem.id}")
            Result.success(domainItem)
        } catch (e: Exception) {
            SystemLogger.error(TAG, "Failed to store item in database", e)
            Result.failure(e)
        }
    }

    override fun getAllItems(): List<Item> {
        return itemDao.getAllSortedByExpirationDate().map(ItemMapper::toDomain)
    }

    override fun deleteItem(id: String): Boolean {
        SystemLogger.info(TAG, "Deleting item id: $id")
        return itemDao.deleteById(id) > 0
    }
}
