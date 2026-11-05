package com.example.expirationtracker.core.logging

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

/**
 * System Logger implementing the mandatory system logging infrastructure per TSK-001 & SRS Technical Constraints.
 * Logs are recorded with timestamps, level, tag, and message.
 *
 * REFACTOR: File I/O persistence is delegated to a dedicated background single-thread executor
 * to guarantee that logging NEVER blocks the main/UI thread (ensuring 60fps per REQ-NF-001).
 */
object SystemLogger {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    @Volatile
    private var logFile: File? = null
    private val logExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "system-logger-thread").apply { isDaemon = true }
    }

    fun init(targetFile: File) {
        targetFile.parentFile?.mkdirs()
        logFile = targetFile
        info("SystemLogger", "System logging initialized. Target file: ${targetFile.absolutePath}")
    }

    fun debug(tag: String, message: String) = log(Level.DEBUG, tag, message)
    fun info(tag: String, message: String) = log(Level.INFO, tag, message)
    fun warn(tag: String, message: String) = log(Level.WARN, tag, message)
    fun error(tag: String, message: String, throwable: Throwable? = null) =
        log(Level.ERROR, tag, message, throwable)

    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val formatted = "[$timestamp] [${level.name}] [$tag] $message"

        println(formatted)
        throwable?.printStackTrace()

        val target = logFile ?: return
        logExecutor.execute {
            try {
                PrintWriter(FileWriter(target, true)).use { writer ->
                    writer.println(formatted)
                    throwable?.printStackTrace(writer)
                }
            } catch (e: Exception) {
                System.err.println("Failed to write to system log file: ${e.message}")
            }
        }
    }
}
