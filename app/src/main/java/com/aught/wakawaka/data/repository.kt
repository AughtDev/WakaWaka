package com.aught.wakawaka.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aught.wakawaka.workers.WakaDataFetchWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


interface WakaDataRepository {
    public val projects: ProjectsRepository
    public val statistics: StatisticsRepository
    public val settings: SettingsRepository
}


interface ProjectsRepository {
    fun getAggregate(): Flow<AggregateData>
    fun get(name: String): Flow<ProjectSpecificData?>
    fun list(): Flow<List<ProjectSpecificData>>
}

interface StatisticsRepository {
    fun get(): StateFlow<WakaStatistics>
}

interface SettingsRepository {
    fun get(): StateFlow<SettingsData>
    fun save(settingsData: SettingsData)
}

class WakaDataRepositoryImpl(
    val context: Context
) : WakaDataRepository {
    private val sharedPreferences =
        context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

    private val _aggregateDataFlow =
        MutableStateFlow(WakaDataFetchWorker.loadAggregateData(context))
    private val aggregateDataFlow: StateFlow<AggregateData> = _aggregateDataFlow


    private val _projectsDataFlow =
        MutableStateFlow(WakaDataFetchWorker.loadProjectSpecificData(context))
    private val projectsDataFlow: StateFlow<Map<String, ProjectSpecificData>> = _projectsDataFlow


    private val _statisticsFlow =
        MutableStateFlow(WakaDataFetchWorker.loadWakaStatistics(context))
    private val statisticsFlow: StateFlow<WakaStatistics> = _statisticsFlow


    private val _settingsFlow =
        MutableStateFlow(WakaDataFetchWorker.loadSettingsData(context))
    private val settingsFlow: StateFlow<SettingsData> = _settingsFlow


    private val sharedPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            Log.d("KaiDataRepositoryImpl", "sharedPrefsListener: $changedKey")
            if (changedKey == null) {
                // Verify this is a clear() by checking if keys are gone
                val hasNoData = !sharedPreferences.contains(WakaHelpers.AGGREGATE_DATA_KEY) &&
                        !sharedPreferences.contains(WakaHelpers.PROJECT_SPECIFIC_DATA_KEY) &&
                        !sharedPreferences.contains(WakaHelpers.WAKA_STATISTICS_KEY) &&
                        !sharedPreferences.contains(WakaHelpers.WAKATIME_API)

                if (hasNoData) {
                    // This is definitely a clear operation
                    _aggregateDataFlow.value = WakaHelpers.INITIAL_AGGREGATE_DATA
                    _projectsDataFlow.value = emptyMap()
                    _statisticsFlow.value = WakaHelpers.INITIAL_WAKA_STATISTICS
                    _settingsFlow.value = SettingsData()
                }
                return@OnSharedPreferenceChangeListener
            }
            when (changedKey) {
                WakaHelpers.AGGREGATE_DATA_KEY -> {
                    val newAggregateData = WakaDataFetchWorker.loadAggregateData(context)
                    _aggregateDataFlow.value = newAggregateData

                    val newSettingsData = WakaDataFetchWorker.loadSettingsData(context)
                    _settingsFlow.value = newSettingsData
                }

                WakaHelpers.PROJECT_SPECIFIC_DATA_KEY -> {
                    val newProjectData = WakaDataFetchWorker.loadProjectSpecificData(context)
                    _projectsDataFlow.value = newProjectData
                }

                WakaHelpers.WAKA_STATISTICS_KEY -> {
                    val newStatisticsData = WakaDataFetchWorker.loadWakaStatistics(context)
                    _statisticsFlow.value = newStatisticsData
                }

                WakaHelpers.WAKATIME_API -> {
                    val newSettingsData = WakaDataFetchWorker.loadSettingsData(context)
                    _settingsFlow.value = newSettingsData
                }
            }
        }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    public override val projects: ProjectsRepository = object : ProjectsRepository {
        override fun getAggregate(): Flow<AggregateData> {
            return aggregateDataFlow
        }

        override fun get(name: String): Flow<ProjectSpecificData?> {
            return projectsDataFlow.map { it[name] }
        }

        override fun list(): Flow<List<ProjectSpecificData>> {
            return projectsDataFlow.map { it.values.toList() }
        }
    }

    public override val statistics: StatisticsRepository = object : StatisticsRepository {
        override fun get(): StateFlow<WakaStatistics> {
            return statisticsFlow
        }
    }

    public override val settings: SettingsRepository = object : SettingsRepository {
        override fun get(): StateFlow<SettingsData> {
            return settingsFlow
        }

        override fun save(settingsData: SettingsData) {
            WakaDataFetchWorker.saveSettingsData(context, settingsData)
        }
    }


}
