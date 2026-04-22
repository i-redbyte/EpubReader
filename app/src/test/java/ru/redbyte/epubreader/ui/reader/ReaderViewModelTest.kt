package ru.redbyte.epubreader.ui.reader

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.redbyte.epubreader.MainDispatcherRule
import ru.redbyte.epubreader.domain.PreparedBook
import ru.redbyte.epubreader.domain.ReadingPosition
import ru.redbyte.epubreader.domain.repository.EpubBookRepository
import ru.redbyte.epubreader.domain.repository.ReadingPositionRepository
import ru.redbyte.epubreader.logging.AppFileLogger
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadBook_emitsReady_whenRepositorySucceeds() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val tmp = File(app.cacheDir, "vm-${System.nanoTime()}").apply { mkdirs() }
        val html = File(tmp, "c.xhtml").apply { writeText("<html><body>x</body></html>") }
        val book = PreparedBook(
            bookId = "bid",
            title = "Title",
            unpackRoot = tmp,
            opfDir = tmp,
            spineFiles = listOf(html),
            tocEntries = emptyList(),
        )
        val vm = ReaderViewModel(
            app,
            FakeEpubRepo(Result.success(book)),
            FakeReadingPositionRepo(null),
            AppFileLogger(app),
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is ReaderUiState.Ready)
    }

    @Test
    fun loadBook_emitsError_whenRepositoryFails() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ReaderViewModel(
            app,
            FakeEpubRepo(Result.failure(IllegalStateException("boom"))),
            FakeReadingPositionRepo(null),
            AppFileLogger(app),
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is ReaderUiState.Error)
    }

    @Test
    fun parseJsFloat_parsesQuotedAndPlain() {
        assertEquals(0.5f, ReaderViewModel.parseJsFloat("\"0.5\"")!!, 0f)
        assertEquals(0.25f, ReaderViewModel.parseJsFloat("0.25")!!, 0f)
        assertTrue(ReaderViewModel.parseJsFloat(null) == null)
        assertTrue(ReaderViewModel.parseJsFloat("null") == null)
    }
}

private class FakeEpubRepo(
    private val result: Result<PreparedBook>,
) : EpubBookRepository {
    override suspend fun prepareDemoBook(): Result<PreparedBook> = result
}

private class FakeReadingPositionRepo(
    initial: ReadingPosition?,
) : ReadingPositionRepository {
    private val flow = MutableStateFlow(initial)
    override val position: Flow<ReadingPosition?> = flow
    override suspend fun save(position: ReadingPosition) {
        flow.value = position
    }
}
