package ru.redbyte.epubreader.data

import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object EpubPathResolver {

    fun stripFragment(href: String): String = href.trim().substringBefore("#")

    fun fragmentPart(href: String): String? {
        if (!href.contains('#')) return null
        val f = href.substringAfter('#')
        return f.takeIf { it.isNotEmpty() }
    }

    fun resolveRelativeToOpf(opfDir: File, epubRoot: File, resourceHref: String): File {
        val pathOnly = stripFragment(resourceHref).removePrefix("/")
        val decoded = runCatching {
            URLDecoder.decode(pathOnly, StandardCharsets.UTF_8.name())
        }.getOrDefault(pathOnly)
        val fromOpf = File(opfDir, decoded).canonicalFile
        if (fromOpf.isFile) return fromOpf
        val fromRoot = File(epubRoot, decoded).canonicalFile
        if (fromRoot.isFile) return fromRoot
        return fromOpf
    }

    fun normalizeForCompare(href: String): String {
        val p = stripFragment(href).trim().removePrefix("/")
        val decoded = runCatching {
            URLDecoder.decode(p, StandardCharsets.UTF_8.name())
        }.getOrDefault(p)
        return decoded.replace('\\', '/')
    }

    fun relativePathFromOpf(opfDir: File, file: File): String? {
        val opf = opfDir.canonicalFile.absolutePath + File.separator
        val f = file.canonicalFile.absolutePath
        if (!f.startsWith(opf)) return null
        return f.removePrefix(opf).replace(File.separatorChar, '/')
    }
}
