package ru.redbyte.epubreader.logging

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Пишет строки в файлы вида `log_<timestamp>.txt` в [directory].
 * Перед созданием нового файла сессии удаляет самые старые логи, чтобы на диске было не больше [maxFiles] файлов.
 */
internal class RotatingFileLogWriter(
    private val directory: File,
    private val maxFiles: Int = DEFAULT_MAX_FILES,
) {

    private val lock = Any()
    private var writer: BufferedWriter? = null

    fun beginNewSession() {
        synchronized(lock) {
            closeWriterLocked()
            directory.mkdirs()
            pruneBeforeNewSessionLocked()
            val name = "log_${formatTimestampForFileName()}.txt"
            val file = File(directory, name)
            writer = BufferedWriter(
                OutputStreamWriter(
                    FileOutputStream(file, true),
                    StandardCharsets.UTF_8
                ),
            )
            appendLineLocked("SESSION", "START", "file=${file.name}")
            writer?.flush()
        }
    }

    fun appendLine(level: String, tag: String, message: String) {
        synchronized(lock) {
            appendLineLocked(level, tag, message)
            writer?.flush()
        }
    }

    fun appendCrash(thread: Thread, throwable: Throwable) {
        synchronized(lock) {
            if (writer == null) {
                runCatching {
                    directory.mkdirs()
                    val f = File(
                        directory,
                        "log_emergency_${System.currentTimeMillis()}.txt"
                    )
                    writer = BufferedWriter(
                        OutputStreamWriter(
                            FileOutputStream(f, true),
                            StandardCharsets.UTF_8
                        ),
                    )
                }
            }
            val sw = java.io.StringWriter()
            java.io.PrintWriter(sw).use { throwable.printStackTrace(it) }
            appendLineLocked(
                "CRASH",
                "UNCAUGHT",
                "thread=${thread.name} ${throwable.javaClass.name}: ${throwable.message}\n${sw}",
            )
            writer?.flush()
        }
    }

    fun close() {
        synchronized(lock) {
            closeWriterLocked()
        }
    }

    private fun appendLineLocked(level: String, tag: String, message: String) {
        val w = writer ?: return
        val ts =
            DateTimeFormatter
                .ISO_OFFSET_DATE_TIME
                .format(ZonedDateTime.now(ZoneId.systemDefault()))
        w.append(ts)
        w.append(' ')
        w.append(level)
        w.append(" [")
        w.append(tag)
        w.append("] ")
        w.append(
            message
                .replace("\r\n", "\n")
                .replace('\r', '\n')
        )
        w.newLine()
    }

    private fun closeWriterLocked() {
        runCatching { writer?.flush() }
        runCatching { writer?.close() }
        writer = null
    }

    private fun pruneBeforeNewSessionLocked() {
        val files = directory.listFiles()?.filter { f ->
            f.isFile && f.name.startsWith(LOG_PREFIX) && f.name.endsWith(LOG_SUFFIX)
        }?.sortedBy { it.lastModified() } ?: return
        val overflow = files.size + 1 - maxFiles
        if (overflow > 0) {
            files.take(overflow).forEach { runCatching { it.delete() } }
        }
    }

    companion object {
        private const val LOG_PREFIX = "log_"
        private const val LOG_SUFFIX = ".txt"
        internal const val DEFAULT_MAX_FILES = 3

        private fun formatTimestampForFileName(): String {
            val z = ZonedDateTime.now(ZoneId.systemDefault())
            return String.format(
                Locale.US,
                "%04d%02d%02d_%02d%02d%02d_%03d",
                z.year,
                z.monthValue,
                z.dayOfMonth,
                z.hour,
                z.minute,
                z.second,
                z.nano / 1_000_000,
            )
        }
    }
}
