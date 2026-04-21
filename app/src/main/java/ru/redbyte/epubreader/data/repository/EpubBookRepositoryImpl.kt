package ru.redbyte.epubreader.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import ru.redbyte.epubreader.data.EpubPathResolver
import ru.redbyte.epubreader.data.Unzipper
import ru.redbyte.epubreader.di.ApplicationContext
import ru.redbyte.epubreader.domain.PreparedBook
import ru.redbyte.epubreader.domain.TocEntry
import ru.redbyte.epubreader.domain.repository.EpubBookRepository
import ru.redbyte.epubreader.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubBookRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : EpubBookRepository {

    private val epubFile: File
        get() = File(context.filesDir, LOCAL_EPUB_NAME)

    private val unpackDir: File
        get() = File(context.filesDir, UNPACK_SUBDIR)

    private data class SpineItem(val href: String, val file: File)

    override suspend fun prepareDemoBook(): Result<PreparedBook> = withContext(Dispatchers.IO) {
        runCatching {
            copyAssetIfNeeded()
            val containerXml = File(unpackDir, "META-INF/container.xml")
            if (!containerXml.isFile) {
                if (unpackDir.exists()) {
                    unpackDir.deleteRecursively()
                }
                unpackDir.mkdirs()
                Unzipper.unzip(epubFile, unpackDir)
            }

            val book = readBook(epubFile)
            val opfHref = book.opfResource.href
            val opfDir = File(unpackDir, opfHref).parentFile
                ?: throw IllegalStateException(context.getString(R.string.error_opf_dir))

            val spineItems = buildFilteredSpine(book, opfDir, unpackDir)
            val spineFiles = spineItems.map { it.file }
            val tocEntries = buildOrderedToc(spineItems)

            val bookId = book.metadata.identifiers.firstOrNull()?.value
                ?: book.metadata.titles.firstOrNull()
                ?: "demo-book"

            PreparedBook(
                bookId = bookId,
                title = book.metadata.titles.firstOrNull()
                    ?: context.getString(R.string.book_default_title),
                unpackRoot = unpackDir,
                opfDir = opfDir,
                spineFiles = spineFiles,
                tocEntries = tocEntries,
            )
        }
    }

    private fun buildFilteredSpine(book: Book, opfDir: File, unpackDir: File): List<SpineItem> {
        val raw = book.spine.spineReferences
        val items = ArrayList<SpineItem>(raw.size)
        for (ref in raw) {
            val res = ref.resource
            if (!isSpineResourceEligible(res)) continue
            val file = EpubPathResolver.resolveRelativeToOpf(opfDir, unpackDir, res.href)
            if (!shouldPresentSpineFile(file)) continue
            items.add(SpineItem(res.href, file))
        }
        if (items.isEmpty()) {
            return raw.map { ref ->
                val res = ref.resource
                SpineItem(res.href, EpubPathResolver.resolveRelativeToOpf(opfDir, unpackDir, res.href))
            }
        }
        return items
    }

    private fun isSpineResourceEligible(r: Resource): Boolean {
        val href = r.href.lowercase()
        if (href.endsWith(".ncx")) return false
        if (href.contains("toc.ncx")) return false
        val mt = r.mediaType?.name?.lowercase().orEmpty()
        if (mt.contains("ncx")) return false
        if (mt.contains("smil")) return false
        if (mt.startsWith("image/")) return false
        return true
    }

    private fun shouldPresentSpineFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        if (file.length() == 0L) return false
        val maxBytes = minOf(file.length(), 96_000L).toInt()
        val text = file.inputStream().use { ins ->
            val buf = ByteArray(maxBytes)
            val n = ins.read(buf, 0, maxBytes)
            if (n <= 0) return false
            String(buf, 0, n, StandardCharsets.UTF_8)
        }
        val body = Regex("<body[^>]*>([\\s\\S]*)</body>", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
        if (body == null) {
            return text.length > 80
        }
        val lower = body.lowercase()
        if (lower.contains("<img") || lower.contains("<svg") || lower.contains("<video") ||
            lower.contains("<audio") || lower.contains("<object")
        ) {
            return true
        }
        val noTags = body.replace(Regex("<[^>]+>"), " ")
        return noTags.any { it.isLetterOrDigit() }
    }

    private fun readBook(file: File): Book {
        FileInputStream(file).use { fis ->
            return EpubReader().readEpub(fis)
        }
    }

    private fun copyAssetIfNeeded() {
        context.assets.open(DEMO_EPUB_ASSET).use { input ->
            FileOutputStream(epubFile).use { out -> input.copyTo(out) }
        }
    }

    private fun buildOrderedToc(items: List<SpineItem>): List<TocEntry> {
        if (items.isEmpty()) return emptyList()
        val out = ArrayList<TocEntry>(items.size)
        out.add(
            TocEntry(
                title = context.getString(R.string.toc_cover_title),
                href = items[0].href,
                depth = 0,
            ),
        )
        for (i in 1 until items.size) {
            out.add(
                TocEntry(
                    title = context.getString(R.string.toc_chapter_format, i),
                    href = items[i].href,
                    depth = 0,
                ),
            )
        }
        return out
    }

    companion object {
        const val DEMO_EPUB_ASSET = "ebook.demo.epub"
        private const val LOCAL_EPUB_NAME = "ebook.demo.epub"
        private const val UNPACK_SUBDIR = "epub_unpack/demo"
    }
}
