package ru.redbyte.epubreader

import android.app.Application
import ru.redbyte.epubreader.di.AppComponent
import ru.redbyte.epubreader.di.DaggerAppComponent

class EpubReaderApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
        val log = appComponent.appFileLogger()
        log.installUncaughtExceptionHandler()
        log.startSession()
        log.i("Application", "onCreate")
    }
}
