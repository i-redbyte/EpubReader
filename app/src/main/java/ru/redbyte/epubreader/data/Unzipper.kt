package ru.redbyte.epubreader.data

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object Unzipper {

    fun unzip(zipFile: File, destDir: File) {
        destDir.mkdirs()
        val canonicalDest = destDir.canonicalFile
        BufferedInputStream(FileInputStream(zipFile)).use { bis ->
            ZipInputStream(bis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.isDirectory) {
                        File(destDir, entry.name).mkdirs()
                    } else {
                        val outFile = File(destDir, entry.name)
                        val parent = outFile.parentFile ?: destDir
                        parent.mkdirs()
                        val canonicalOut = outFile.canonicalFile
                        if (!canonicalOut.path
                            .startsWith(canonicalDest.path + File.separator)
                            && canonicalOut.path != canonicalDest.path
                        ) {
                            throw SecurityException("Zip path traversal: ${entry.name}")
                        }
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }
}
