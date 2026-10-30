import unittest
import os
import sqlite3
from src.logger import logger, LOG_FILE

class TestTSK001ArchitectureDBLogging(unittest.TestCase):

    def test_logger_initialization_and_writing(self):
        """Verify logger is initialized and creates log file entry."""
        test_msg = "TSK-001 Logger Test Message"
        logger.info(test_msg)
        
        self.assertTrue(os.path.exists(LOG_FILE), "Log file should exist in logs/")
        with open(LOG_FILE, "r") as f:
            log_content = f.read()
        self.assertIn(test_msg, log_content)

    def test_database_connection_and_schema(self):
        """Verify database connection can be established and core tables initialized."""
        from src.database import Database  # Expected module for DB setup

        db = Database(":memory:")
        db.init_db()

        conn = db.get_connection()
        cursor = conn.cursor()

        # Check items table existence
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='items';")
        table = cursor.fetchone()
        self.assertIsNotNone(table, "Table 'items' must exist in SQLite database.")

        # Check settings table existence
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='settings';")
        table = cursor.fetchone()
        self.assertIsNotNone(table, "Table 'settings' must exist in SQLite database.")

    def test_database_transactional_integrity(self):
        """Verify transactional integrity when errors occur during database operations."""
        from src.database import Database

        db = Database(":memory:")
        db.init_db()

        # Attempt invalid SQL operation inside a transaction block to ensure rollback