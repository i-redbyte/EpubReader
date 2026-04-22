package ru.redbyte.epubreader.ui.reader

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.redbyte.epubreader.domain.repository.EpubBookRepository
import ru.redbyte.epubreader.domain.repository.ReadingPositionRepository
import ru.redbyte.epubreader.logging.AppFileLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderViewModelFactory @Inject constructor(
    private val application: Application,
    private val epubBookRepository: EpubBookRepository,
    private val readingPositionRepository: ReadingPositionRepository,
    private val appFileLogger: AppFileLogger,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return ReaderViewModel(
                application,
                epubBookRepository,
                readingPositionRepository,
                appFileLogger,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
