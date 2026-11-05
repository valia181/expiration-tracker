package com.example.expirationtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper
import com.example.expirationtracker.data.repository.ItemRepositoryImpl
import com.example.expirationtracker.ui.screen.ItemListScreen
import com.example.expirationtracker.ui.theme.ExpirationTrackerTheme
import com.example.expirationtracker.ui.viewmodel.ItemListViewModel
import com.example.expirationtracker.ui.viewmodel.ItemListViewModelFactory
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: ExpirationTrackerDbHelper
    private lateinit var itemDao: ItemDao
    private lateinit var repository: ItemRepositoryImpl

    private val viewModel: ItemListViewModel by viewModels {
        ItemListViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize logging infrastructure per SRS Technical Constraints
        val logFile = File(filesDir, "logs/system.log")
        SystemLogger.init(logFile)
        SystemLogger.info("MainActivity", "Application starting. Initializing database and repository.")

        // Initialize SQLite Database, DAO, and Repository
        dbHelper = ExpirationTrackerDbHelper(applicationContext)
        itemDao = ItemDao(dbHelper)
        repository = ItemRepositoryImpl(itemDao)

        setContent {
            ExpirationTrackerTheme {
                ItemListScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dbHelper.isInitialized) {
            dbHelper.close()
            SystemLogger.info("MainActivity", "Database connection closed.")
        }
    }
}
