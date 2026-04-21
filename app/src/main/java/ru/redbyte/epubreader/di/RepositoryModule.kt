package ru.redbyte.epubreader.di

import dagger.Binds
import dagger.Module
import ru.redbyte.epubreader.data.repository.EpubBookRepositoryImpl
import ru.redbyte.epubreader.data.repository.ReadingPositionRepositoryImpl
import ru.redbyte.epubreader.domain.repository.EpubBookRepository
import ru.redbyte.epubreader.domain.repository.ReadingPositionRepository
import javax.inject.Singleton

@Module
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindEpubBookRepository(impl: EpubBookRepositoryImpl): EpubBookRepository

    @Binds
    @Singleton
    fun bindReadingPositionRepository(impl: ReadingPositionRepositoryImpl): ReadingPositionRepository
}
