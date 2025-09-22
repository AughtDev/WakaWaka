package com.aught.wakawaka.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aught.wakawaka.data.SettingsData
import com.aught.wakawaka.data.WakaDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val wakaDataRepository: WakaDataRepository
): ViewModel() {
    val settings = wakaDataRepository.settings.get().stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsData()
    )

    fun saveSettings(settingsData: SettingsData) {
        wakaDataRepository.settings.save(settingsData)
    }
}
