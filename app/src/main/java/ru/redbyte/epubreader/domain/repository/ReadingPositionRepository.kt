package ru.redbyte.epubreader.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.redbyte.epubreader.domain.ReadingPosition

interface ReadingPositionRepository {
    val position: Flow<ReadingPosition?>
    suspend fun save(position: ReadingPosition)
}
