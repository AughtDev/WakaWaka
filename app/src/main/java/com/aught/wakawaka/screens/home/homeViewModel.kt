package com.aught.wakawaka.screens.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DataState
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.TargetStreakData
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataUseCase
import com.aught.wakawaka.data.WakaHelpers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

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
    private val wakaDataUseCase: WakaDataUseCase
) : ViewModel() {

    // region GLOBAL DATA
    // ? ........................

    val projects: StateFlow<List<ProjectSpecificData>> = wakaDataUseCase.getProjects().stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val aggregateData: StateFlow<AggregateData> = wakaDataUseCase.getAggregate().stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WakaHelpers.INITIAL_AGGREGATE_DATA
    )

    // ? ........................
    // endregion ........................



    // region HOME UI DATA
    // ? ........................

//    private val _uiState = MutableStateFlow(HomeUIState())
//    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()
//
//    fun selectProject(projectName: String) {
//        _uiState.update { it.copy(isLoading = true, selectedProjectName = projectName) }
//
//        viewModelScope.launch {
//            val dataRequest = if (projectName == WakaHelpers.ALL_PROJECTS_ID) {
//                DataRequest.Aggregate
//            } else {
//                DataRequest.ProjectSpecific(projectName)
//            }
//
//            // 1. Start all data fetching in parallel using async
//            val durationMapDeferred = async {
//                // Wait for a non-empty map before finishing
//                wakaDataTransformer.getLabelledDurationStats(dataRequest).first()
//            }
//            val dateMapDeferred = async {
//                wakaDataTransformer.getDateToDurationData(dataRequest).first()
//            }
//            val colorDeferred = async {
//                // Wait for a non-null color
//                wakaDataTransformer.getProjectColor(dataRequest).first()
//            }
//            val dailyStreakDeferred = async {
//                // Wait for a streak that has been properly initialized
//                wakaDataTransformer.getTargetStreak(dataRequest, TimePeriod.DAY).first()
//            }
//            val weeklyStreakDeferred = async {
//                wakaDataTransformer.getTargetStreak(dataRequest, TimePeriod.WEEK).first()
//            }
//
//            Log.d("HomeViewModel", "Fetching data for project: $projectName, streak deferred: ${dailyStreakDeferred.await()}")
//
//            // 2. Await all the results. This suspends the coroutine until all fetches are complete.
//            _uiState.update {
//                it.copy(
//                    durationLabelValueMap = durationMapDeferred.await(),
//                    dateToDurationMap = dateMapDeferred.await(),
//                    projectColor = colorDeferred.await(),
//                    dailyTargetStreakData = dailyStreakDeferred.await(),
//                    weeklyTargetStreakData = weeklyStreakDeferred.await(),
//                    isLoading = false
//                )
//            }
//        }
//    }

    private val _selectedProjectName = MutableStateFlow(WakaHelpers.ALL_PROJECTS_ID)

    // --- Create dependent flows using flatMapLatest ---
    // This will automatically cancel the old transformer call and start a new one
    // whenever _selectedProjectName changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val projectSpecificDataFlow: Flow<HomeUIState> =
        _selectedProjectName.flatMapLatest { projectName ->
            // Create the DataRequest inside the flow
            val dataRequest = if (projectName == WakaHelpers.ALL_PROJECTS_ID) {
                DataRequest.Aggregate
            } else {
                DataRequest.ProjectSpecific(projectName)
            }

            // Combine all the flows that depend on the dataRequest
            combine(
                wakaDataUseCase.getLabelledDurationStats(dataRequest),
                wakaDataUseCase.getDateToDurationData(dataRequest),
                wakaDataUseCase.getProjectColor(dataRequest),
                wakaDataUseCase.getTargetStreak(dataRequest, TimePeriod.DAY),
                wakaDataUseCase.getTargetStreak(dataRequest, TimePeriod.WEEK)
            ) { durationMapState, dateMapState, colorState, dailyStreakState, weeklyStreakState ->

                // We only proceed if ALL data has successfully loaded
                if (
                    durationMapState is DataState.Success &&
                    dateMapState is DataState.Success &&
                    colorState is DataState.Success &&
                    dailyStreakState is DataState.Success &&
                    weeklyStreakState is DataState.Success
                ) {
                    HomeUIState(
                        selectedProjectName = projectName,
                        durationLabelValueMap = durationMapState.data,
                        dateToDurationMap = dateMapState.data,
                        projectColor = colorState.data,
                        dailyTargetStreakData = dailyStreakState.data,
                        weeklyTargetStreakData = weeklyStreakState.data,
                        isLoading = false
                    )
                } else {
                    // If any stream is still loading, reflect that in the state
                    HomeUIState(selectedProjectName = projectName, isLoading = true)
                }
            }
        }

    // --- The Final `uiState` for the UI ---
    val uiState: StateFlow<HomeUIState> = projectSpecificDataFlow
        .scan(HomeUIState(isLoading = true)) { previous, current ->
            // Only log when loading state changes
            if (current.isLoading) {
                previous.copy(
                    isLoading = true
                )
            } else {
                current
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUIState(isLoading = true)
        )

    fun selectProject(projectName: String) {
        _selectedProjectName.value = projectName
    }

    init {
        selectProject(WakaHelpers.ALL_PROJECTS_ID)
    }


    // ? ........................
    // endregion ........................


}
