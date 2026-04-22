package ru.redbyte.epubreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class EpubPathResolverTest {

    @Test
    fun stripFragment_removesHash() {
        assertEquals("chapter.xhtml", EpubPathResolver.stripFragment("chapter.xhtml#sec1"))
        assertEquals("a", EpubPathResolver.stripFragment("a"))
    }

    @Test
    fun fragmentPart_returnsAfterHash() {
        assertEquals("sec1", EpubPathResolver.fragmentPart("chapter.xhtml#sec1"))
        assertNull(EpubPathResolver.fragmentPart("chapter.xhtml"))
    }

    @Test
    fun normalizeForCompare_decodesAndNormalizesSlashes() {
        assertEquals(
            "OPS/chapter.xhtml",
            EpubPathResolver.normalizeForCompare("/OPS/chapter.xhtml")
        )
    }

    @Test
    fun resolveRelativeToOpf_prefersFileUnderOpfDir() {
        val root = File.createTempFile("epub", "root").parentFile!!
        val opfDir = File(root, "OPS").apply { mkdirs() }
        val chapter = File(opfDir, "ch1.xhtml").apply { writeText("<html/>") }
        val resolved = EpubPathResolver.resolveRelativeToOpf(opfDir, root, "ch1.xhtml")
        assertEquals(chapter.canonicalFile, resolved.canonicalFile)
    }

    @Test
    fun relativePathFromOpf_returnsPosixPath() {
        val root = File.createTempFile("epub2", "root").parentFile!!
        val opfDir = File(root, "OPS").apply { mkdirs() }
        val chapter = File(opfDir, "ch1.xhtml").apply { writeText("x") }
        assertEquals("ch1.xhtml", EpubPathResolver.relativePathFromOpf(opfDir, chapter))
    }
}
