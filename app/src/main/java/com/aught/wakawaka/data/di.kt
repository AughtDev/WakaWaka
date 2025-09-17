package com.aught.wakawaka.data

import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val wakaAppModule = module {
    single<WakaDataRepository> {
        WakaDataRepositoryImpl(androidContext())
    }

}

