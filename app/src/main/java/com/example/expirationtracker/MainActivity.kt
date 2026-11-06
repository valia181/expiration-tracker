package com.example.expirationtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper
import com.example.expirationtracker.data.repository.ItemRepositoryImpl
import com.example.expirationtracker.notification.NotificationScheduler
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

        // Schedule background WorkManager periodic notification checks (REQ-NF-004, AC-NF-001)
        NotificationScheduler.schedulePeriodicCheck(this)

        setContent {
            ExpirationTrackerTheme {
                RequestNotificationPermissionEffect()
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

@Composable
private fun RequestNotificationPermissionEffect() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                SystemLogger.info("MainActivity", "POST_NOTIFICATIONS runtime permission granted by user.")
            } else {
                SystemLogger.warn("MainActivity", "POST_NOTIFICATIONS runtime permission denied by user.")
            }
        }

        LaunchedEffect(Unit) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                SystemLogger.info("MainActivity", "Requesting POST_NOTIFICATIONS runtime permission.")
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
