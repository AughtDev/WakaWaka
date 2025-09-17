package com.aught.wakawaka
import android.app.Application
import com.aught.wakawaka.data.wakaAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class WakaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//         Initialize Koin or any other dependency injection framework here

        startKoin {
            androidContext(this@WakaApplication)
            modules(wakaAppModule)
        }
    }
}
