package ru.redbyte.epubreader.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import ru.redbyte.epubreader.ui.reader.ReaderViewModelFactory
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        RepositoryModule::class,
    ],
)
interface AppComponent {

    fun readerViewModelFactory(): ReaderViewModelFactory

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: Application): AppComponent
    }
}
