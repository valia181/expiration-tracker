package com.example.expirationtracker.data.local.dao

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.sqlite.transaction
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper
import com.example.expirationtracker.data.local.entity.ItemEntity
import com.example.expirationtracker.domain.model.Category

/**
 * Data Access Object (DAO) for items table implementing SQLite operations with transactional integrity.
 * Traceability: TSK-001, REQ-NF-003, REQ-NF-002 (<100ms query performance).
 */
open class ItemDao(val dbHelper: ExpirationTrackerDbHelper? = null) {

    companion object {
        private const val TAG = "ItemDao"
        private val ALL_COLUMNS = arrayOf(
            ExpirationTrackerDbHelper.COLUMN_ID,
            ExpirationTrackerDbHelper.COLUMN_TITLE,
            ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE,
            ExpirationTrackerDbHelper.COLUMN_CATEGORY,
            ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION,
            ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR
        )
    }

    open fun insert(item: ItemEntity): Long {
        val db = dbHelper?.writableDatabase ?: return -1L
        var rowId = -1L
        db.transaction {
            val values = ContentValues(6).apply {
                put(ExpirationTrackerDbHelper.COLUMN_ID, item.id)
                put(ExpirationTrackerDbHelper.COLUMN_TITLE, item.title)
                put(ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE, item.expirationDate)
                put(ExpirationTrackerDbHelper.COLUMN_CATEGORY, item.category)
                put(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION, item.storageLocation)
                put(ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR, item.statusColor)
            }
            rowId = insertOrThrow(ExpirationTrackerDbHelper.TABLE_ITEMS, null, values)
        }
        SystemLogger.info(TAG, "Inserted item with id: ${item.id}")
        return rowId
    }

    open fun getAllSortedByExpirationDate(): List<ItemEntity> {
        val startTime = System.currentTimeMillis()
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val items = mutableListOf<ItemEntity>()

        val cursor: Cursor = db.query(
            ExpirationTrackerDbHelper.TABLE_ITEMS,
            ALL_COLUMNS,
            null,
            null,
            null,
            null,
            "${ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE} ASC"
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_ID)
            val titleCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_TITLE)
            val expCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE)
            val catCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_CATEGORY)
            val locCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION)
            val statusCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR)

            while (it.moveToNext()) {
                val loc = if (it.isNull(locCol)) null else it.getString(locCol)
                items.add(
                    ItemEntity(
                        id = it.getString(idCol),
                        title = it.getString(titleCol),
                        expirationDate = it.getString(expCol),
                        category = it.getString(catCol),
                        storageLocation = loc,
                        statusColor = it.getString(statusCol)
                    )
                )
            }
        }
        val duration = System.currentTimeMillis() - startTime
        SystemLogger.debug(TAG, "Loaded ${items.size} items from database in ${duration}ms.")
        return items
    }

    open fun searchAndFilterItems(search: String? = null, category: String? = null): List<ItemEntity> {
        val startTime = System.currentTimeMillis()
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val items = mutableListOf<ItemEntity>()

        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (!search.isNullOrBlank()) {
            selectionParts.add("${ExpirationTrackerDbHelper.COLUMN_TITLE} LIKE ?")
            selectionArgs.add("%${search.trim()}%")
        }

        if (!category.isNullOrBlank() && !category.equals("All", ignoreCase = true)) {
            val dbCategoryValue = Category.toDbValue(category) ?: category.trim()
            selectionParts.add("UPPER(${ExpirationTrackerDbHelper.COLUMN_CATEGORY}) = UPPER(?)")
            selectionArgs.add(dbCategoryValue)
        }

        val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(" AND ") else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        val cursor: Cursor = db.query(
            ExpirationTrackerDbHelper.TABLE_ITEMS,
            ALL_COLUMNS,
            selection,
            args,
            null,
            null,
            "${ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE} ASC"
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_ID)
            val titleCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_TITLE)
            val expCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE)
            val catCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_CATEGORY)
            val locCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION)
            val statusCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR)

            while (it.moveToNext()) {
                val loc = if (it.isNull(locCol)) null else it.getString(locCol)
                items.add(
                    ItemEntity(
                        id = it.getString(idCol),
                        title = it.getString(titleCol),
                        expirationDate = it.getString(expCol),
                        category = it.getString(catCol),
                        storageLocation = loc,
                        statusColor = it.getString(statusCol)
                    )
                )
            }
        }
        val duration = System.currentTimeMillis() - startTime
        SystemLogger.debug(TAG, "Queried ${items.size} items matching search='$search', category='$category' in ${duration}ms.")
        return items
    }

    open fun deleteById(id: String): Int {
        val db = dbHelper?.writableDatabase ?: return 0
        var deletedRows = 0
        db.transaction {
            deletedRows = delete(
                ExpirationTrackerDbHelper.TABLE_ITEMS,
                "${ExpirationTrackerDbHelper.COLUMN_ID} = ?",
                arrayOf(id)
            )
        }
        SystemLogger.info(TAG, "Deleted item id: $id, affected rows: $deletedRows")
        return deletedRows
    }

    open fun update(item: ItemEntity): Int {
        val db = dbHelper?.writableDatabase ?: return 0
        var updatedRows = 0
        db.transaction {
            val values = ContentValues(5).apply {
                put(ExpirationTrackerDbHelper.COLUMN_TITLE, item.title)
                put(ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE, item.expirationDate)
                put(ExpirationTrackerDbHelper.COLUMN_CATEGORY, item.category)
                put(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION, item.storageLocation)
                put(ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR, item.statusColor)
            }
            updatedRows = update(
                ExpirationTrackerDbHelper.TABLE_ITEMS,
                values,
                "${ExpirationTrackerDbHelper.COLUMN_ID} = ?",
                arrayOf(item.id)
            )
        }
        SystemLogger.info(TAG, "Updated item id: ${item.id}, affected rows: $updatedRows")
        return updatedRows
    }

    open fun deleteAll(): Int {
        val db = dbHelper?.writableDatabase ?: return 0
        var deletedRows = 0
        db.transaction {
            deletedRows = delete(ExpirationTrackerDbHelper.TABLE_ITEMS, null, null)
        }
        SystemLogger.info(TAG, "Cleared all items from database. Affected rows: $deletedRows")
        return deletedRows
    }

    open fun restoreFromBackup(backupEntities: List<ItemEntity>): Boolean {
        val db = dbHelper?.writableDatabase ?: return false
        var success = false
        try {
            db.transaction {
                // 1. Delete all existing items
                delete(ExpirationTrackerDbHelper.TABLE_ITEMS, null, null)

                // 2. Insert all backup entities transactionally
                for (item in backupEntities) {
                    val values = ContentValues(6).apply {
                        put(ExpirationTrackerDbHelper.COLUMN_ID, item.id)
                        put(ExpirationTrackerDbHelper.COLUMN_TITLE, item.title)
                        put(ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE, item.expirationDate)
                        put(ExpirationTrackerDbHelper.COLUMN_CATEGORY, item.category)
                        put(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION, item.storageLocation)
                        put(ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR, item.statusColor)
                    }
                    val rowId = insert(ExpirationTrackerDbHelper.TABLE_ITEMS, null, values)
                    if (rowId == -1L) {
                        throw IllegalStateException("Failed to insert item ${item.id} during backup restore.")
                    }
                }
            }
            success = true
            SystemLogger.info(TAG, "Database transactionally restored with ${backupEntities.size} items.")
        } catch (e: Exception) {
            SystemLogger.error(TAG, "Database restore transaction failed and rolled back.", e)
            success = false
        }
        return success
    }
}
