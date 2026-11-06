package com.example.expirationtracker.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expirationtracker.core.logging.SystemLogger
import java.util.concurrent.TimeUnit

/**
 * Utility to schedule persistent background notification checks via WorkManager.
 * Traceability: REQ-NF-004, AC-NF-001.
 */
object NotificationScheduler {

    private const val TAG = "NotificationScheduler"
    const val WORK_NAME = "expiration_notification_periodic_work"

    fun schedulePeriodicCheck(context: Context) {
        SystemLogger.info(TAG, "Scheduling persistent periodic WorkManager notification check.")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ExpirationNotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        SystemLogger.info(TAG, "Successfully enqueued unique periodic work '$WORK_NAME'.")
    }
}
