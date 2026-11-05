package com.example.expirationtracker.core.logging

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * System Logger implementing the mandatory system logging infrastructure per TSK-001 & SRS Technical Constraints.
 * Logs are recorded with timestamps, level, tag, and message, and written to persistent log files.
 */
object SystemLogger {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    private var logFile: File? = null
    private val lock = Any()

    fun init(targetFile: File) {
        synchronized(lock) {
            targetFile.parentFile?.mkdirs()
            logFile = targetFile
        }
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

        synchronized(lock) {
            logFile?.let { file ->
                try {
                    PrintWriter(FileWriter(file, true)).use { writer ->
                        writer.println(formatted)
                        throwable?.printStackTrace(writer)
                    }
                } catch (e: Exception) {
                    System.err.println("Failed to write to system log file: ${e.message}")
                }
            }
        }
    }
}
