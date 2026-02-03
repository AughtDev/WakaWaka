package com.aught.wakawaka.screens.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.CheatType
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DataState
import com.aught.wakawaka.data.ProjectCheatCounts
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUIState(
    val selectedProjectName: String = WakaHelpers.ALL_PROJECTS_ID,
    val durationLabelValueMap: Map<String, Int> = emptyMap(),
    val dateToDurationMap: Map<String, Int> = emptyMap(),
    val projectColor: Color? = null,
    val dailyTargetStreakData: TargetStreakData = TargetStreakData(null, 0, 0f, false),
    val weeklyTargetStreakData: TargetStreakData = TargetStreakData(null, 0, 0f, false),
    val cheatCounts: ProjectCheatCounts? = null,
    val isLoading: Boolean = false,
    val unloaded: Boolean = true
)

class HomeViewModel(
    private val wakaDataUseCase: WakaDataUseCase
) : ViewModel() {

    // region GLOBAL DATA
    // ? ........................

    val projects: StateFlow<List<ProjectSpecificData>> = wakaDataUseCase.getProjects(true).stateIn(
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
                wakaDataUseCase.getCheatCounts(dataRequest),
                wakaDataUseCase.getTargetStreak(dataRequest, TimePeriod.DAY),
                wakaDataUseCase.getTargetStreak(dataRequest, TimePeriod.WEEK),
//            ) { durationMapState, dateMapState, colorState, cheatCountsState, dailyStreakState, weeklyStreakState ->
                ) { states: Array<DataState<*>> ->
                val durationMapState = states[0] as DataState<Map<String, Int>>
                val dateMapState = states[1] as DataState<Map<String, Int>>
                val colorState = states[2] as DataState<Color?>
                val cheatCountsState = states[3] as DataState<ProjectCheatCounts>
                val dailyStreakState = states[4] as DataState<TargetStreakData>
                val weeklyStreakState = states[5] as DataState<TargetStreakData>

                // We only proceed if ALL data has successfully loaded
                if (
                    durationMapState is DataState.Success &&
                    dateMapState is DataState.Success &&
                    colorState is DataState.Success &&
                    cheatCountsState is DataState.Success &&
                    dailyStreakState is DataState.Success &&
                    weeklyStreakState is DataState.Success
                ) {
                    HomeUIState(
                        selectedProjectName = projectName,
                        durationLabelValueMap = durationMapState.data,
                        dateToDurationMap = dateMapState.data,
                        projectColor = colorState.data,
                        cheatCounts = cheatCountsState.data,
                        dailyTargetStreakData = dailyStreakState.data,
                        weeklyTargetStreakData = weeklyStreakState.data,
                        isLoading = false, unloaded = false
                    )
                } else {
                    // If any stream is still loading, reflect that in the state
                    HomeUIState(
                        selectedProjectName = projectName,
                        unloaded = false,
                        isLoading = true
                    )
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
            initialValue = HomeUIState(isLoading = true, unloaded = true)
        )

    fun selectProject(projectName: String) {
        _selectedProjectName.value = projectName
        // update
    }

    init {
        selectProject(WakaHelpers.ALL_PROJECTS_ID)
    }

    // region CHEAT FUNCTIONS
    // ? ........................

    fun useCheat(type: CheatType, date: LocalDate, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val dataRequest = if (_selectedProjectName.value == WakaHelpers.ALL_PROJECTS_ID) {
                DataRequest.Aggregate
            } else {
                DataRequest.ProjectSpecific(_selectedProjectName.value)
            }
            val result = wakaDataUseCase.useCheat(dataRequest, type, date)
            onResult(result)
        }
    }

    fun useCheat(type: CheatType, date: LocalDate, projectName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val dataRequest = DataRequest.ProjectSpecific(projectName)
            val result = wakaDataUseCase.useCheat(dataRequest, type, date)
            onResult(result)
        }
    }

    fun getBlockedCheatDates(type: CheatType): Flow<Set<String>> {
        val dataRequest = if (_selectedProjectName.value == WakaHelpers.ALL_PROJECTS_ID) {
            DataRequest.Aggregate
        } else {
            DataRequest.ProjectSpecific(_selectedProjectName.value)
        }
        return wakaDataUseCase.getBlockedCheatDates(dataRequest, type)
    }

    fun getUsedCheatDates(type: CheatType): Flow<List<String>> {
        val dataRequest = if (_selectedProjectName.value == WakaHelpers.ALL_PROJECTS_ID) {
            DataRequest.Aggregate
        } else {
            DataRequest.ProjectSpecific(_selectedProjectName.value)
        }
        return wakaDataUseCase.getUsedCheatDates(dataRequest, type)
    }

    fun getUsedCheatDates(type: CheatType, projectName: String): Flow<List<String>> {
        val dataRequest = DataRequest.ProjectSpecific(projectName)
        return wakaDataUseCase.getUsedCheatDates(dataRequest, type)
    }

    fun getCheatCounts(projectName: String): Flow<ProjectCheatCounts?> {
        val dataRequest = DataRequest.ProjectSpecific(projectName)
        // extract the ProjectCheatCounts? from the DataState
        return wakaDataUseCase.getCheatCounts(dataRequest).map { dataState ->
            if (dataState is DataState.Success) {
                dataState.data
            } else {
                null
            }
        }
    }

    // ? ........................
    // endregion ........................


    // ? ........................
    // endregion ........................


}
