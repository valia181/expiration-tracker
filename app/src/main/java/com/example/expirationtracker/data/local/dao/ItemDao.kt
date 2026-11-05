package com.example.expirationtracker.data.local.dao

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.sqlite.transaction
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper
import com.example.expirationtracker.data.local.entity.ItemEntity

/**
 * Data Access Object (DAO) for items table implementing SQLite operations with transactional integrity.
 * Traceability: TSK-001, REQ-NF-003.
 */
class ItemDao(private val dbHelper: ExpirationTrackerDbHelper) {

    fun insert(item: ItemEntity): Long {
        val db = dbHelper.writableDatabase
        var rowId = -1L
        db.transaction {
            val values = ContentValues().apply {
                put(ExpirationTrackerDbHelper.COLUMN_ID, item.id)
                put(ExpirationTrackerDbHelper.COLUMN_TITLE, item.title)
                put(ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE, item.expirationDate)
                put(ExpirationTrackerDbHelper.COLUMN_CATEGORY, item.category)
                put(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION, item.storageLocation)
                put(ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR, item.statusColor)
            }
            rowId = insertOrThrow(ExpirationTrackerDbHelper.TABLE_ITEMS, null, values)
        }
        SystemLogger.info("ItemDao", "Inserted item with id: ${item.id}")
        return rowId
    }

    fun getAllSortedByExpirationDate(): List<ItemEntity> {
        val db = dbHelper.readableDatabase
        val items = mutableListOf<ItemEntity>()
        val cursor: Cursor = db.query(
            ExpirationTrackerDbHelper.TABLE_ITEMS,
            null,
            null,
            null,
            null,
            null,
            "${ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToEntity(it))
            }
        }
        SystemLogger.debug("ItemDao", "Loaded ${items.size} items from database.")
        return items
    }

    fun deleteById(id: String): Int {
        val db = dbHelper.writableDatabase
        var deletedRows = 0
        db.transaction {
            deletedRows = delete(
                ExpirationTrackerDbHelper.TABLE_ITEMS,
                "${ExpirationTrackerDbHelper.COLUMN_ID} = ?",
                arrayOf(id)
            )
        }
        SystemLogger.info("ItemDao", "Deleted item id: $id, affected rows: $deletedRows")
        return deletedRows
    }

    fun deleteAll(): Int {
        val db = dbHelper.writableDatabase
        var deletedRows = 0
        db.transaction {
            deletedRows = delete(ExpirationTrackerDbHelper.TABLE_ITEMS, null, null)
        }
        SystemLogger.info("ItemDao", "Cleared all items from database. Affected rows: $deletedRows")
        return deletedRows
    }

    private fun cursorToEntity(cursor: Cursor): ItemEntity {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_TITLE))
        val expirationDate = cursor.getString(cursor.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_EXPIRATION_DATE))
        val category = cursor.getString(cursor.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_CATEGORY))
        val storageLocation = if (cursor.isNull(cursor.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION))) {
            null
        } else {
            cursor.getString(cursor.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_STORAGE_LOCATION))
        }
        val statusColor = cursor.getString(cursor.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_STATUS_COLOR))

        return ItemEntity(
            id = id,
            title = title,
            expirationDate = expirationDate,
            category = category,
            storageLocation = storageLocation,
            statusColor = statusColor
        )
    }
}
