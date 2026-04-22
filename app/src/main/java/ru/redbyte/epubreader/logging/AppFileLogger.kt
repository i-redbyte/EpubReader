package ru.redbyte.epubreader.logging

import android.content.Context
import ru.redbyte.epubreader.di.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFileLogger @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val writer = RotatingFileLogWriter(
        directory = File(context.filesDir, LOG_DIR_NAME),
        maxFiles = RotatingFileLogWriter.DEFAULT_MAX_FILES,
    )

    @Volatile
    private var crashHandlerInstalled = false

    fun startSession() {
        writer.beginNewSession()
    }

    fun installUncaughtExceptionHandler() {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writer.appendCrash(thread, throwable)
            } catch (_: Throwable) {
                /* no-op */
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun d(tag: String, message: String) {
        writer.appendLine("D", tag, message)
    }

    fun i(tag: String, message: String) {
        writer.appendLine("I", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val text = if (throwable != null) {
            "$message: ${throwable.javaClass.simpleName}: ${throwable.message}"
        } else {
            message
        }
        writer.appendLine("W", tag, text)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val text = if (throwable != null) {
            val sw = java.io.StringWriter()
            java.io.PrintWriter(sw).use { throwable.printStackTrace(it) }
            "$message\n${sw}"
        } else {
            message
        }
        writer.appendLine("E", tag, text)
    }

    companion object {
        private const val LOG_DIR_NAME = "app_logs"
    }
}
