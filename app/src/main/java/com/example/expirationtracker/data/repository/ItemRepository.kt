package com.example.expirationtracker.data.repository

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.mapper.ItemMapper
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.model.StatusColor
import com.example.expirationtracker.domain.validation.ItemValidator
import com.example.expirationtracker.domain.validation.ValidationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository interface defining item data access operations.
 * Traceability: REQ-NF-007 (Architecture separation).
 */
interface ItemRepository {
    suspend fun createItem(input: ItemInput): Result<Item>
    suspend fun getAllItems(search: String? = null, category: String? = null): List<Item>
    suspend fun deleteItem(id: String): Boolean
}

/**
 * Concrete ItemRepository implementation delegating to SQLite DAO with IO dispatcher execution.
 */
class ItemRepositoryImpl(
    private val itemDao: ItemDao,
    private val validator: ItemValidator = ItemValidator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ItemRepository {

    companion object {
        private const val TAG = "ItemRepository"
    }

    override suspend fun createItem(input: ItemInput): Result<Item> = withContext(ioDispatcher) {
        SystemLogger.info(TAG, "Attempting to create item with title: '${input.title}'")

        val validationResult: ValidationResult = validator.validate(input)
        if (!validationResult.isValid) {
            val errorMsg = validationResult.errors.joinToString("; ")
            SystemLogger.warn(TAG, "Item creation aborted: $errorMsg")
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
        }

        val domainItem = Item(
            title = input.title.trim(),
            expirationDate = input.expirationDate.trim(),
            category = input.category,
            storageLocation = input.storageLocation?.trim()?.ifBlank { null },
            statusColor = StatusColor.GREEN
        )

        val entity = ItemMapper.toEntity(domainItem)

        try {
            itemDao.insert(entity)
            SystemLogger.info(TAG, "Item successfully stored with ID: ${domainItem.id}")
            Result.success(domainItem)
        } catch (e: Exception) {
            SystemLogger.error(TAG, "Failed to store item in database", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllItems(search: String?, category: String?): List<Item> = withContext(ioDispatcher) {
        if (search.isNullOrBlank() && category.isNullOrBlank()) {
            itemDao.getAllSortedByExpirationDate().map(ItemMapper::toDomain)
        } else {
            itemDao.searchAndFilterItems(search, category).map(ItemMapper::toDomain)
        }
    }

    override suspend fun deleteItem(id: String): Boolean = withContext(ioDispatcher) {
        SystemLogger.info(TAG, "Deleting item id: $id")
        itemDao.deleteById(id) > 0
    }
}
