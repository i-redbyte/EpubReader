package ru.redbyte.epubreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.redbyte.epubreader.di.ApplicationContext
import ru.redbyte.epubreader.domain.ReadingPosition
import ru.redbyte.epubreader.domain.repository.ReadingPositionRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.readingPositionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reading_position",
)

@Singleton
class ReadingPositionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReadingPositionRepository {

    private val dataStore get() = context.readingPositionDataStore

    private val keyBookId = stringPreferencesKey("book_id")
    private val keySpineIndex = intPreferencesKey("spine_index")
    private val keyScrollRatio = floatPreferencesKey("scroll_ratio")

    override val position: Flow<ReadingPosition?> = dataStore.data.map { prefs ->
        val id = prefs[keyBookId] ?: return@map null
        ReadingPosition(
            bookId = id,
            spineIndex = prefs[keySpineIndex] ?: 0,
            scrollRatio = prefs[keyScrollRatio] ?: 0f,
        )
    }

    override suspend fun save(position: ReadingPosition) {
        dataStore.edit { prefs ->
            prefs[keyBookId] = position.bookId
            prefs[keySpineIndex] = position.spineIndex
            prefs[keyScrollRatio] = position.scrollRatio.coerceIn(0f, 1f)
        }
    }
}
