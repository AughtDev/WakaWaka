package com.aught.wakawaka.screens.home

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DataState
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.TargetStreakData
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataTransformer
import com.aught.wakawaka.data.WakaHelpers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUIState(
    val selectedProjectName: String = WakaHelpers.ALL_PROJECTS_ID,
    val durationLabelValueMap: Map<String, Int> = emptyMap(),
    val dateToDurationMap: Map<String, Int> = emptyMap(),
    val projectColor: Color? = null,
    val dailyTargetStreakData: TargetStreakData = TargetStreakData(null, 0, 0f, false),
    val weeklyTargetStreakData: TargetStreakData = TargetStreakData(null, 0, 0f, false),
    val isLoading: Boolean = false,
)

class HomeViewModel(
    private val wakaDataTransformer: WakaDataTransformer
) : ViewModel() {

    // region GLOBAL DATA
    // ? ........................

    val projects: StateFlow<List<ProjectSpecificData>> = wakaDataTransformer.getProjects().stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val aggregateData: StateFlow<AggregateData> = wakaDataTransformer.getAggregate().stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WakaHelpers.INITIAL_AGGREGATE_DATA
    )

    // ? ........................
    // endregion ........................



    // region HOME UI DATA
    // ? ........................

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

    fun selectProject(projectName: String) {
        _uiState.update { it.copy(isLoading = true, selectedProjectName = projectName) }

        viewModelScope.launch {
            val dataRequest = if (projectName == WakaHelpers.ALL_PROJECTS_ID) {
                DataRequest.Aggregate
            } else {
                DataRequest.ProjectSpecific(projectName)
            }

            // 1. Start all data fetching in parallel using async
            val durationMapDeferred = async {
                // Wait for a non-empty map before finishing
                wakaDataTransformer.getLabelledDurationStats(dataRequest).first()
            }
            val dateMapDeferred = async {
                wakaDataTransformer.getDateToDurationData(dataRequest).first()
            }
            val colorDeferred = async {
                // Wait for a non-null color
                wakaDataTransformer.getProjectColor(dataRequest).first()
            }
            val dailyStreakDeferred = async {
                // Wait for a streak that has been properly initialized
                wakaDataTransformer.getTargetStreak(dataRequest, TimePeriod.DAY).first()
            }
            val weeklyStreakDeferred = async {
                wakaDataTransformer.getTargetStreak(dataRequest, TimePeriod.WEEK).first()
            }

            Log.d("HomeViewModel", "Fetching data for project: $projectName, streak deferred: ${dailyStreakDeferred.await()}")

            // 2. Await all the results. This suspends the coroutine until all fetches are complete.
            _uiState.update {
                it.copy(
                    durationLabelValueMap = durationMapDeferred.await(),
                    dateToDurationMap = dateMapDeferred.await(),
                    projectColor = colorDeferred.await(),
                    dailyTargetStreakData = dailyStreakDeferred.await(),
                    weeklyTargetStreakData = weeklyStreakDeferred.await(),
                    isLoading = false
                )
            }
        }
    }

    init {
        selectProject(WakaHelpers.ALL_PROJECTS_ID)
    }


    // ? ........................
    // endregion ........................


}
