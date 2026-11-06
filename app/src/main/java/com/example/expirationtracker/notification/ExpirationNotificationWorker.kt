package com.example.expirationtracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.expirationtracker.R
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper
import com.example.expirationtracker.data.repository.ItemRepository
import com.example.expirationtracker.data.repository.ItemRepositoryImpl
import com.example.expirationtracker.domain.calculator.StatusColorCalculator
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.StatusColor
import com.example.expirationtracker.domain.usecase.GetSortedItemsWithStatusUseCase
import java.time.LocalDate

/**
 * Background WorkManager CoroutineWorker checking for items approaching expiration
 * and triggering unique local Android push notifications per item.
 *
 * Clean Architecture Guarantee:
 * - Delegates all data fetching strictly to ItemRepository layer.
 * - Generates unique notification IDs per item to prevent overwriting notifications.
 *
 * Traceability: REQ-NF-004, AC-NF-001, REQ-NF-007.
 */
class ExpirationNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val repository: ItemRepository = ItemRepositoryImpl(ItemDao(ExpirationTrackerDbHelper(appContext)))
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ExpirationNotificationWorker"
        const val CHANNEL_ID = "expiration_notifications"
        const val CHANNEL_NAME = "Expiration Notifications"
        const val NOTIFICATION_ID_BASE = 1000
    }

    private val getSortedItemsUseCase = GetSortedItemsWithStatusUseCase(StatusColorCalculator())

    override suspend fun doWork(): ListenableWorker.Result {
        SystemLogger.info(TAG, "ExpirationNotificationWorker woke up for background execution.")

        return try {
            val leadTimeDays = repository.getLeadTime()
            val rawItems = repository.getAllItems()
            val today = LocalDate.now()

            val itemsWithStatus = getSortedItemsUseCase.execute(
                items = rawItems,
                currentDate = today,
                leadTimeDays = leadTimeDays
            )

            // Identify items in warning (Orange) or expired (Red) state
            val expiringItems = itemsWithStatus.filter {
                it.statusColor == StatusColor.ORANGE || it.statusColor == StatusColor.RED
            }

            SystemLogger.info(
                TAG,
                "Found ${expiringItems.size} items in expiration window out of ${rawItems.size} total items."
            )

            if (expiringItems.isNotEmpty()) {
                triggerUniqueNotificationsForItems(expiringItems)
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            SystemLogger.error(TAG, "Error executing background notification worker", e)
            ListenableWorker.Result.failure()
        }
    }

    private fun triggerUniqueNotificationsForItems(items: List<Item>) {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for items approaching expiration or expired"
            }
            notificationManager.createNotificationChannel(channel)
        }

        for (item in items) {
            // Unique notification ID per item derived deterministically from item.id
            val uniqueNotificationId = NOTIFICATION_ID_BASE + (item.id.hashCode() and 0x7FFFFFFF) % 100000

            val statusLabel = if (item.statusColor == StatusColor.RED) "Expired" else "Expiring Soon"
            val titleText = "${item.title} ($statusLabel)"
            val contentText = "Expires: ${item.expirationDate} | Category: ${item.category.displayName}"

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(uniqueNotificationId, notification)
            SystemLogger.info(
                TAG,
                "Triggered unique notification (ID=$uniqueNotificationId) for item '${item.title}' (ID=${item.id})."
            )
        }
    }
}
