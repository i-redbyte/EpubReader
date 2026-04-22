package ru.redbyte.epubreader.ui.reader

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.redbyte.epubreader.R
import ru.redbyte.epubreader.data.EpubPathResolver
import ru.redbyte.epubreader.domain.PreparedBook
import ru.redbyte.epubreader.domain.ReadingPosition
import ru.redbyte.epubreader.domain.TocEntry
import ru.redbyte.epubreader.domain.repository.EpubBookRepository
import ru.redbyte.epubreader.domain.repository.ReadingPositionRepository
import ru.redbyte.epubreader.logging.AppFileLogger
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipException

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Error(val message: String) : ReaderUiState
    data class Ready(
        val book: PreparedBook,
        val spineIndex: Int,
        val fileUrl: String,
        val toc: List<TocEntry>,
        val pendingScrollRatio: Float?,
    ) : ReaderUiState
}

private const val TAG = "__ReaderViewModel"

class ReaderViewModel(
    private val application: Application,
    private val epubBookRepository: EpubBookRepository,
    private val readingPositionRepository: ReadingPositionRepository,
    private val appFileLogger: AppFileLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        loadBook()
    }

    fun loadBook() {
        viewModelScope.launch {
            appFileLogger.i(TAG, "loadBook started")
            _uiState.value = ReaderUiState.Loading
            val result = epubBookRepository.prepareDemoBook()
            result.fold(
                onSuccess = { book ->
                    appFileLogger.i(
                        TAG,
                        "book prepared id=${book.bookId} spine=${book.spineFiles.size}"
                    )
                    val saved =
                        runCatching { readingPositionRepository.position.first() }.getOrNull()
                    val spineIndex = if (saved?.bookId == book.bookId) {
                        saved
                            .spineIndex
                            .coerceIn(
                                0,
                                book.spineFiles.lastIndex.coerceAtLeast(0)
                            )
                    } else {
                        0
                    }
                    val pending = if (saved?.bookId == book.bookId) {
                        saved.scrollRatio.coerceIn(0f, 1f)
                    } else {
                        null
                    }
                    runCatching {
                        applyReady(book, spineIndex, pending, urlFragment = null)
                    }.onSuccess {
                        appFileLogger.i(TAG, "state Ready spineIndex=$spineIndex")
                    }.onFailure { e ->
                        appFileLogger.e(TAG, "applyReady failed", e)
                        _uiState.value = ReaderUiState.Error(e.toUserMessage())
                    }
                },
                onFailure = { e ->
                    appFileLogger.e(TAG, "prepareDemoBook failed", e)
                    _uiState.value = ReaderUiState.Error(e.toUserMessage())
                },
            )
        }
    }

    fun selectSpine(index: Int) {
        val state = _uiState.value
        if (state !is ReaderUiState.Ready) return
        val book = state.book
        runCatching {
            val i = index.coerceIn(0, book.spineFiles.lastIndex.coerceAtLeast(0))
            appFileLogger.i(TAG, "selectSpine index=$i")
            applyReady(book, i, null, urlFragment = null)
        }.onFailure { e ->
            appFileLogger.e(TAG, "selectSpine failed", e)
            _uiState.value = ReaderUiState.Error(e.toUserMessage())
        }
    }

    fun selectTocEntry(entry: TocEntry) {
        val state = _uiState.value
        if (state !is ReaderUiState.Ready) return
        val book = state.book
        runCatching {
            appFileLogger.i(TAG, "selectTocEntry href=${entry.href}")
            val idx = findSpineIndexForHref(book, entry.href)
            val frag = EpubPathResolver.fragmentPart(entry.href)
            applyReady(book, idx, null, urlFragment = frag)
        }.onFailure { e ->
            appFileLogger.e(TAG, "selectTocEntry failed", e)
            _uiState.value = ReaderUiState.Error(e.toUserMessage())
        }
    }

    fun onScrollRestoreApplied() {
        val state = _uiState.value
        if (state !is ReaderUiState.Ready) return
        if (state.pendingScrollRatio != null) {
            _uiState.value = state.copy(pendingScrollRatio = null)
        }
    }

    fun saveScrollFromWebView(webView: WebView) {
        val state = _uiState.value
        if (state !is ReaderUiState.Ready) return
        val book = state.book
        webView.evaluateJavascript(SCROLL_RATIO_JS) { raw ->
            val ratio = parseJsFloat(raw) ?: return@evaluateJavascript
            viewModelScope.launch {
                runCatching {
                    readingPositionRepository.save(
                        ReadingPosition(
                            bookId = book.bookId,
                            spineIndex = state.spineIndex,
                            scrollRatio = ratio,
                        ),
                    )
                }.onFailure { err ->
                    appFileLogger.e(TAG, "save position failed", err)
                    _snackbarMessages.tryEmit(application.getString(R.string.error_position_save))
                }
            }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is FileNotFoundException -> application.getString(R.string.error_file_not_found)
        is ZipException -> application.getString(R.string.error_zip)
        is SecurityException -> application.getString(R.string.error_zip)
        is IOException -> application.getString(R.string.error_io)
        else -> message?.takeIf { it.isNotBlank() }
            ?: application.getString(R.string.error_unknown)
    }

    private fun applyReady(
        book: PreparedBook,
        spineIndex: Int,
        pendingScrollRatio: Float?,
        urlFragment: String?,
    ) {
        val idx = spineIndex.coerceIn(0, book.spineFiles.lastIndex.coerceAtLeast(0))
        val file = book.spineFiles[idx]
        if (!file.isFile) {
            throw IllegalStateException(application.getString(R.string.error_open_book))
        }
        val baseUri = file.toURI().toString()
        val url = if (!urlFragment.isNullOrEmpty()) "$baseUri#$urlFragment" else baseUri
        _uiState.value = ReaderUiState.Ready(
            book = book,
            spineIndex = idx,
            fileUrl = url,
            toc = book.tocEntries,
            pendingScrollRatio = pendingScrollRatio,
        )
    }

    private fun findSpineIndexForHref(book: PreparedBook, href: String): Int {
        val targetFile = EpubPathResolver
            .resolveRelativeToOpf(book.opfDir, book.unpackRoot, href)
        book.spineFiles.forEachIndexed { index, file ->
            if (file.canonicalFile == targetFile) return index
        }
        val normalizedTarget = EpubPathResolver.normalizeForCompare(href)
        book.spineFiles.forEachIndexed { index, file ->
            val rel =
                EpubPathResolver.relativePathFromOpf(book.opfDir, file) ?: return@forEachIndexed
            if (rel == normalizedTarget) return index
        }
        val targetName = normalizedTarget.substringAfterLast('/')
        book.spineFiles.forEachIndexed { index, file ->
            if (file.name == targetName) return index
        }
        return 0
    }

    companion object {
        private val SCROLL_RATIO_JS = """
            (function(){
            // Returns current vertical scroll as a fraction of scrollable distance (0..1).
            var h=document.documentElement.scrollHeight-window.innerHeight;
            if(h<=0)return 0;
            return window.scrollY/h;
            })()
        """.trimIndent()

        fun parseJsFloat(raw: String?): Float? {
            if (raw == null || raw == "null") return null
            val s = raw.trim().removeSurrounding("\"")
            return s.toFloatOrNull()
        }

        fun scrollToRatioJs(ratio: Float): String {
            val r = ratio.coerceIn(0f, 1f)
            return """
                (function(r){
                // Scrolls so that r in [0,1] maps to the full vertical scroll range.
                var go=function(){
                // Recompute height (images/fonts may still be loading).
                var de=document.documentElement;var b=document.body;
                var sh=Math.max(de.scrollHeight,b?b.scrollHeight:0);
                var h=sh-window.innerHeight;
                if(h<=0)return;
                window.scrollTo(0,h*r);
                };
                go();
                // Repeat while layout stabilizes; also run after window load.
                setTimeout(go,50);setTimeout(go,150);setTimeout(go,400);setTimeout(go,800);
                if(document.readyState==='complete'){setTimeout(go,0);}
                else{window.addEventListener('load',function(){setTimeout(go,0);});}
                })($r)
            """.trimIndent()
        }
    }
}
