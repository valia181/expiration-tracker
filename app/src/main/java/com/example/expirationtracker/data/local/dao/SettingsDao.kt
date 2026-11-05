package com.example.expirationtracker.data.local.dao

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.sqlite.transaction
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper

/**
 * Data Access Object (DAO) for settings table in SQLite.
 * Traceability: TSK-010, TSK-011, REQ-F-008.
 */
class SettingsDao(private val dbHelper: ExpirationTrackerDbHelper? = null) {

    companion object {
        private const val TAG = "SettingsDao"
    }

    open fun getLeadTime(): Int {
        val db = dbHelper?.readableDatabase ?: return ExpirationTrackerDbHelper.DEFAULT_LEAD_TIME_DAYS
        var leadTime = ExpirationTrackerDbHelper.DEFAULT_LEAD_TIME_DAYS
        val cursor: Cursor = db.query(
            ExpirationTrackerDbHelper.TABLE_SETTINGS,
            arrayOf(ExpirationTrackerDbHelper.COLUMN_SETTING_VALUE),
            "${ExpirationTrackerDbHelper.COLUMN_SETTING_KEY} = ?",
            arrayOf(ExpirationTrackerDbHelper.KEY_NOTIFICATION_LEAD_TIME),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val valCol = it.getColumnIndexOrThrow(ExpirationTrackerDbHelper.COLUMN_SETTING_VALUE)
                val strVal = it.getString(valCol)
                leadTime = strVal?.toIntOrNull() ?: ExpirationTrackerDbHelper.DEFAULT_LEAD_TIME_DAYS
            }
        }
        SystemLogger.debug(TAG, "Loaded notification lead time from DB: $leadTime")
        return leadTime
    }

    open fun saveLeadTime(leadTime: Int) {
        val db = dbHelper?.writableDatabase ?: return
        db.transaction {
            val values = ContentValues().apply {
                put(ExpirationTrackerDbHelper.COLUMN_SETTING_KEY, ExpirationTrackerDbHelper.KEY_NOTIFICATION_LEAD_TIME)
                put(ExpirationTrackerDbHelper.COLUMN_SETTING_VALUE, leadTime.toString())
            }
            insertWithOnConflict(
                ExpirationTrackerDbHelper.TABLE_SETTINGS,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
        }
        SystemLogger.info(TAG, "Saved notification lead time to DB: $leadTime")
    }
}
