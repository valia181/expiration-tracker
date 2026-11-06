package com.example.expirationtracker.data.repository

import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.dao.SettingsDao
import com.example.expirationtracker.data.mapper.ItemMapper
import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.BackupData
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.domain.model.Settings
import com.example.expirationtracker.domain.model.StatusColor
import com.example.expirationtracker.domain.validation.ItemValidator
import com.example.expirationtracker.domain.validation.ValidationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository interface defining item, settings, and backup data access operations.
 * Traceability: REQ-NF-007 (Architecture separation), REQ-F-008, REQ-F-009, REQ-F-010, REQ-F-012, REQ-NF-003.
 */
interface ItemRepository {
    suspend fun createItem(input: ItemInput): Result<Item>
    suspend fun updateItem(id: String, input: ItemInput): Result<Item>
    suspend fun getAllItems(search: String? = null, category: String? = null): List<Item>
    suspend fun deleteItem(id: String): Boolean
    suspend fun getLeadTime(): Int
    suspend fun saveLeadTime(leadTime: Int): Result<Unit>
    suspend fun exportBackupData(): BackupData
    suspend fun importBackupData(backupData: BackupData): Boolean
}

/**
 * Concrete ItemRepository implementation delegating to SQLite DAO with IO dispatcher execution.
 */
class ItemRepositoryImpl(
    private val itemDao: ItemDao,
    private val settingsDao: SettingsDao = SettingsDao(itemDao.dbHelper),
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

    override suspend fun updateItem(id: String, input: ItemInput): Result<Item> = withContext(ioDispatcher) {
        SystemLogger.info(TAG, "Attempting to update item ID: $id with title: '${input.title}'")

        val validationResult: ValidationResult = validator.validate(input)
        if (!validationResult.isValid) {
            val errorMsg = validationResult.errors.joinToString("; ")
            SystemLogger.warn(TAG, "Item update aborted: $errorMsg")
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
        }

        val leadTimeDays = settingsDao.getLeadTime()
        val today = LocalDate.now()
        val parsedDate = try {
            LocalDate.parse(input.expirationDate.trim())
        } catch (_: Exception) {
            today
        }
        val statusColor = StatusColorCalculator().calculateStatus(
            expirationDate = parsedDate,
            currentDate = today,
            leadTimeDays = leadTimeDays
        )

        val domainItem = Item(
            id = id,
            title = input.title.trim(),
            expirationDate = input.expirationDate.trim(),
            category = input.category,
            storageLocation = input.storageLocation?.trim()?.ifBlank { null },
            statusColor = statusColor
        )

        val entity = ItemMapper.toEntity(domainItem)

        try {
            val affectedRows = itemDao.update(entity)
            if (affectedRows > 0) {
                SystemLogger.info(TAG, "Item successfully updated with ID: $id")
                Result.success(domainItem)
            } else {
                SystemLogger.warn(TAG, "Item with ID $id not found in database for update.")
                Result.failure(NoSuchElementException("Item with ID '$id' was not found."))
            }
        } catch (e: Exception) {
            SystemLogger.error(TAG, "Failed to update item in database", e)
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

    override suspend fun getLeadTime(): Int = withContext(ioDispatcher) {
        settingsDao.getLeadTime()
    }

    override suspend fun saveLeadTime(leadTime: Int): Result<Unit> = withContext(ioDispatcher) {
        if (!Settings.isValidLeadTime(leadTime)) {
            SystemLogger.warn(TAG, "Attempted to save invalid lead time: $leadTime")
            return@withContext Result.failure(IllegalArgumentException("Invalid lead time $leadTime. Allowed values are [1, 3, 7, 14]."))
        }
        try {
            settingsDao.saveLeadTime(leadTime)
            SystemLogger.info(TAG, "Successfully saved lead time: $leadTime")
            Result.success(Unit)
        } catch (e: Exception) {
            SystemLogger.error(TAG, "Failed to save lead time", e)
            Result.failure(e)
        }
    }

    override suspend fun exportBackupData(): BackupData = withContext(ioDispatcher) {
        val leadTime = settingsDao.getLeadTime()
        val items = itemDao.getAllSortedByExpirationDate().map(ItemMapper::toDomain)
        SystemLogger.info(TAG, "Exported backup data: leadTime=$leadTime, itemsCount=${items.size}")
        BackupData(
            settings = Settings(notificationLeadTime = leadTime),
            items = items
        )
    }

    override suspend fun importBackupData(backupData: BackupData): Boolean = withContext(ioDispatcher) {
        if (!Settings.isValidLeadTime(backupData.settings.notificationLeadTime)) {
            SystemLogger.warn(TAG, "Import backup aborted: invalid notificationLeadTime '${backupData.settings.notificationLeadTime}'")
            return@withContext false
        }

        try {
            // Save settings
            settingsDao.saveLeadTime(backupData.settings.notificationLeadTime)

            // Save items in transactional block via DAO (REQ-NF-003)
            val entities = backupData.items.map(ItemMapper::toEntity)
            val restored = itemDao.restoreFromBackup(entities)
            if (restored) {
                SystemLogger.info(TAG, "Backup import transaction completed successfully for ${entities.size} items.")
            } else {
                SystemLogger.warn(TAG, "Backup import transaction failed.")
            }
            restored
        } catch (e: Exception) {
            SystemLogger.error(TAG, "Exception during backup import, transaction rolled back.", e)
            false
        }
    }
}
