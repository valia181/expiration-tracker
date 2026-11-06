package com.example.expirationtracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.expirationtracker.core.logging.SystemLogger
import com.example.expirationtracker.notification.ExpirationNotificationWorker

/**
 * Custom Application class initializing notification channels and global system infrastructure on startup.
 */
class ExpirationTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ExpirationNotificationWorker.CHANNEL_ID,
                ExpirationNotificationWorker.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for items approaching expiration or expired"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            SystemLogger.info(
                "ExpirationTrackerApplication",
                "Created NotificationChannel '${ExpirationNotificationWorker.CHANNEL_ID}' with IMPORTANCE_HIGH."
            )
        }
    }
}
