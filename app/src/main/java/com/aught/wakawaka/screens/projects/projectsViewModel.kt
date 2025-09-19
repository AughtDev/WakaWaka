package com.aught.wakawaka.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.WakaDataTransformer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProjectsViewModel(
    private val wakaDataTransformer: WakaDataTransformer
) : ViewModel() {
    val projects: StateFlow<List<ProjectSpecificData>> = wakaDataTransformer.getProjects(true).stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}
