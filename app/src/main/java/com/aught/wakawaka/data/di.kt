package com.aught.wakawaka.data

import com.aught.wakawaka.screens.home.HomeViewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel

val wakaAppModule = module {
    single<WakaDataRepository> {
        WakaDataRepositoryImpl(androidContext())
    }

    factory<WakaDataTransformer> {
        WakaDataTransformer(get())
    }

    viewModel { HomeViewModel(get()) }
}

