package com.example.expirationtracker.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.example.expirationtracker.core.logging.SystemLogger

/**
 * SQLite Database Helper providing schema creation, index optimizations, migrations,
 * and transactional integrity per REQ-NF-003, REQ-NF-002, and TSK-001.
 */
class ExpirationTrackerDbHelper(
    context: Context,
    dbName: String = DATABASE_NAME
) : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "expiration_tracker.db"
        const val DATABASE_VERSION = 1

        // Table: items
        const val TABLE_ITEMS = "items"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_EXPIRATION_DATE = "expiration_date"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_STORAGE_LOCATION = "storage_location"
        const val COLUMN_STATUS_COLOR = "status_color"

        // Index on expiration_date for <100ms chronological sorting and date filtering (REQ-NF-002)
        const val INDEX_EXPIRATION_DATE = "idx_items_expiration_date"
        const val INDEX_CATEGORY = "idx_items_category"

        // Table: settings
        const val TABLE_SETTINGS = "settings"
        const val COLUMN_SETTING_KEY = "setting_key"
        const val COLUMN_SETTING_VALUE = "setting_value"
        const val KEY_NOTIFICATION_LEAD_TIME = "notification_lead_time"
        const val DEFAULT_LEAD_TIME_DAYS = 7

        private const val SQL_CREATE_ITEMS_TABLE = """
            CREATE TABLE $TABLE_ITEMS (
                $COLUMN_ID TEXT PRIMARY KEY NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_EXPIRATION_DATE TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_STORAGE_LOCATION TEXT,
                $COLUMN_STATUS_COLOR TEXT NOT NULL
            );
        """

        private const val SQL_CREATE_INDEX_EXPIRATION_DATE = """
            CREATE INDEX IF NOT EXISTS $INDEX_EXPIRATION_DATE 
            ON $TABLE_ITEMS ($COLUMN_EXPIRATION_DATE ASC);
        """

        private const val SQL_CREATE_INDEX_CATEGORY = """
            CREATE INDEX IF NOT EXISTS $INDEX_CATEGORY 
            ON $TABLE_ITEMS ($COLUMN_CATEGORY);
        """

        private const val SQL_CREATE_SETTINGS_TABLE = """
            CREATE TABLE $TABLE_SETTINGS (
                $COLUMN_SETTING_KEY TEXT PRIMARY KEY NOT NULL,
                $COLUMN_SETTING_VALUE TEXT NOT NULL
            );
        """

        private const val SQL_INSERT_DEFAULT_SETTINGS = """
            INSERT INTO $TABLE_SETTINGS ($COLUMN_SETTING_KEY, $COLUMN_SETTING_VALUE)
            VALUES ('$KEY_NOTIFICATION_LEAD_TIME', '$DEFAULT_LEAD_TIME_DAYS');
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        SystemLogger.info("ExpirationTrackerDbHelper", "Creating database schema and performance indexes.")
        try {
            db.transaction {
                execSQL(SQL_CREATE_ITEMS_TABLE)
                execSQL(SQL_CREATE_INDEX_EXPIRATION_DATE)
                execSQL(SQL_CREATE_INDEX_CATEGORY)
                execSQL(SQL_CREATE_SETTINGS_TABLE)
                execSQL(SQL_INSERT_DEFAULT_SETTINGS)
            }
            SystemLogger.info("ExpirationTrackerDbHelper", "Database schema and indexes created successfully.")
        } catch (e: Exception) {
            SystemLogger.error("ExpirationTrackerDbHelper", "Failed to create database schema", e)
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        SystemLogger.warn(
            "ExpirationTrackerDbHelper",
            "Database upgrade requested from version $oldVersion to $newVersion"
        )
    }
}
