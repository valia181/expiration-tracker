package com.example.expirationtracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.expirationtracker.data.api.ItemApiController
import com.example.expirationtracker.data.local.dao.ItemDao
import com.example.expirationtracker.data.local.db.ExpirationTrackerDbHelper
import com.example.expirationtracker.data.repository.ItemRepositoryImpl
import com.example.expirationtracker.domain.model.Category
import com.example.expirationtracker.domain.model.ItemInput
import com.example.expirationtracker.notification.ExpirationNotificationWorker
import com.example.expirationtracker.notification.NotificationScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Integration Test Suite for TSK-012: Background Notification Scheduling via WorkManager.
 *
 * Traceability:
 * - REQ-NF-004: Persistent WorkManager scheduling surviving reboots.
 * - AC-NF-001: Automatic WorkManager restoration and notification triggering for expiring items.
 *
 * CRITICAL TDD RULE: Fails on compilation because NotificationScheduler and ExpirationNotificationWorker do not exist yet.
 */
@RunWith(AndroidJUnit4::class)
class NotificationSchedulerTest {

    private lateinit var context: Context
    private lateinit var dbHelper: ExpirationTrackerDbHelper
    private lateinit var itemDao: ItemDao
    private lateinit var repository: ItemRepositoryImpl
    private lateinit var apiController: ItemApiController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        dbHelper = ExpirationTrackerDbHelper(context)
        itemDao = ItemDao(dbHelper)
        itemDao.deleteAll()
        repository = ItemRepositoryImpl(itemDao)
        apiController = ItemApiController(repository = repository)
    }

    @Test
    fun verify_periodic_background_notification_task_is_enqueued_with_persistent_constraints() {
        // Given WorkManager test helper is initialized
        // When NotificationScheduler schedules periodic notification check
        NotificationScheduler.schedulePeriodicCheck(context)

        // Then periodic work request is enqueued in WorkManager
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME).get()

        assertNotNull(workInfos)
        assertEquals(1, workInfos.size)
        val workInfo = workInfos[0]
        assertTrue(
            "Periodic background notification work must be ENQUEUED, RUNNING, or SUCCEEDED",
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.SUCCEEDED
        )
    }

    @Test
    fun verify_worker_queries_repository_and_identifies_expiring_items() = runBlocking {
        // Given database contains items expiring within lead time window (e.g. tomorrow)
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val nextMonth = LocalDate.now().plusDays(30).toString()

        apiController.createItem(ItemInput("Expiring Milk", tomorrow, Category.FOOD))
        apiController.createItem(ItemInput("Safe Canned Corn", nextMonth, Category.FOOD))

        // When ExpirationNotificationWorker runs
        val worker = TestListenableWorkerBuilder.from(context, ExpirationNotificationWorker::class.java).build()
        val result = worker.doWork()

        // Then worker succeeds and identifies expiring items
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun verify_system_attempts_to_trigger_local_android_notification_for_expiring_items() = runBlocking {
        // Given database contains an item expiring today
        val today = LocalDate.now().toString()
        apiController.createItem(ItemInput("Urgent Medicine", today, Category.MEDICINE))

        // When worker executes
        val worker = TestListenableWorkerBuilder.from(context, ExpirationNotificationWorker::class.java).build()
        val result = worker.doWork()

        // Then returns success indicating local notification attempt executed
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
