package com.example.expirationtracker.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.expirationtracker.core.logging.SystemLogger

/**
 * SQLite Database Helper providing schema creation, migrations, and transactional integrity
 * per REQ-NF-003 and TSK-001.
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
        SystemLogger.info("ExpirationTrackerDbHelper", "Creating database tables: $TABLE_ITEMS, $TABLE_SETTINGS")
        db.beginTransaction()
        try {
            db.execSQL(SQL_CREATE_ITEMS_TABLE)
            db.execSQL(SQL_CREATE_SETTINGS_TABLE)
            db.execSQL(SQL_INSERT_DEFAULT_SETTINGS)
            db.setTransactionSuccessful()
            SystemLogger.info("ExpirationTrackerDbHelper", "Database schema created successfully.")
        } catch (e: Exception) {
            SystemLogger.error("ExpirationTrackerDbHelper", "Failed to create database schema", e)
            throw e
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        SystemLogger.warn(
            "ExpirationTrackerDbHelper",
            "Database upgrade requested from version $oldVersion to $newVersion"
        )
    }
}
