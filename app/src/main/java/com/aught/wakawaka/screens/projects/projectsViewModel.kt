package com.aught.wakawaka.screens.projects

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataTransformers
import com.aught.wakawaka.data.WakaDataUseCase
import com.aught.wakawaka.data.getPeriodicDates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProjectGraphData(
    val name: String,
    val data: List<Int>,
    val dates: List<LocalDate>,
    val targetInHours: Float?,
    val streak: Int,
    val hitTarget: Boolean,
    val excludedDays: Set<Int>,
    val color: Color
)

class ProjectsViewModel(
    private val wakaDataUseCase: WakaDataUseCase
) : ViewModel() {
    val projects: StateFlow<List<ProjectSpecificData>> = wakaDataUseCase.getProjects(true).stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun getProjectGraphData(
        name: String,
        timePeriod: TimePeriod,
    ): ProjectGraphData? {
        val project = projects.value.find { it.name == name } ?: return null

        val target = WakaDataTransformers.getProjectTarget(project,timePeriod)
        val streak = WakaDataTransformers.getProjectStreak(project,timePeriod)
        val completion = WakaDataTransformers.calcCompletion(
            target, WakaDataTransformers.calcOffsetPeriodicDurationInSeconds(project.dailyDurationInSeconds,timePeriod,0)
            )
        val excludedDays = WakaDataTransformers.getProjectExcludedDays(project,timePeriod)

        return ProjectGraphData(
            name = name,
            data = WakaDataTransformers.calcPeriodicDurationsInSeconds(project.dailyDurationInSeconds,timePeriod,7),
            dates = getPeriodicDates(timePeriod, 7),
            targetInHours = target,
            streak = streak.count,
            hitTarget = completion >= 1f,
            excludedDays = excludedDays,
            color = WakaDataTransformers.getProjectColor(project) ?: Color.Gray
        )
    }
}
