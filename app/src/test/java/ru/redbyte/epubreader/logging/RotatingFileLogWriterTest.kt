package ru.redbyte.epubreader.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RotatingFileLogWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun beginNewSession_createsFileWithLogPrefix() {
        val dir = tempFolder.root
        val w = RotatingFileLogWriter(dir, maxFiles = 3)
        w.beginNewSession()
        w.appendLine("I", "Test", "hello")
        w.close()
        val files =
            dir.listFiles()?.filter { it.name.startsWith("log_") && it.name.endsWith(".txt") }
                .orEmpty()
        assertEquals(1, files.size)
        val text = files.single().readText()
        assertTrue(text.contains("[Test]"))
        assertTrue(text.contains("hello"))
        assertTrue(text.contains("SESSION"))
    }

    @Test
    fun prune_keepsAtMostMaxFiles() {
        val dir = tempFolder.root
        repeat(3) { i ->
            val f = File(dir, "log_2026010${i}_120000_${i}00.txt")
            f.createNewFile()
            f.setLastModified(10_000L * (i + 1))
        }
        assertEquals(3, dir.listFiles()!!.size)
        val w = RotatingFileLogWriter(dir, maxFiles = 3)
        w.beginNewSession()
        w.close()
        val logs =
            dir.listFiles()!!.filter { it.name.startsWith("log_") && it.name.endsWith(".txt") }
        assertEquals(3, logs.size)
    }

    @Test
    fun appendCrash_writesWhenWriterMissing_opensEmergencyFile() {
        val dir = tempFolder.root
        val w = RotatingFileLogWriter(dir, maxFiles = 3)
        w.appendCrash(Thread.currentThread(), IllegalStateException("x"))
        w.close()
        val emergency = dir.listFiles()!!.single { it.name.startsWith("log_emergency_") }
        assertTrue(emergency.readText().contains("CRASH"))
        assertTrue(emergency.readText().contains("IllegalStateException"))
    }
}
