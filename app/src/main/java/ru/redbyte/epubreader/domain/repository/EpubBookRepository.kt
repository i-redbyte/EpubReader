package ru.redbyte.epubreader.domain.repository

import ru.redbyte.epubreader.domain.PreparedBook

interface EpubBookRepository {
    suspend fun prepareDemoBook(): Result<PreparedBook>
}
