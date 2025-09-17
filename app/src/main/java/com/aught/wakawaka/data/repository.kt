package com.aught.wakawaka.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aught.wakawaka.workers.WakaDataFetchWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface WakaDataRepository {
    public val projects: ProjectsRepository
}


interface ProjectsRepository {
    fun getAggregate(): AggregateData

    fun get(name: String): ProjectSpecificData?
    fun getLive(name: String): StateFlow<ProjectSpecificData?>

    fun list(): List<ProjectSpecificData>
    fun listLive(): StateFlow<List<ProjectSpecificData>>
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

    private val sharedPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            Log.d("KaiDataRepositoryImpl", "sharedPrefsListener: $changedKey")
            when (changedKey) {
                WakaHelpers.AGGREGATE_DATA_KEY -> {
                    val newAggregateData = WakaDataFetchWorker.loadAggregateData(context)
                    _aggregateDataFlow.value = newAggregateData
                }

                WakaHelpers.PROJECT_SPECIFIC_DATA_KEY -> {
                    val newProjectData = WakaDataFetchWorker.loadProjectSpecificData(context)
                    _projectsDataFlow.value = newProjectData
                }
            }
        }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    public override val projects: ProjectsRepository = object : ProjectsRepository {
        override fun getAggregate(): AggregateData {
            return aggregateDataFlow.value
        }

        override fun get(name: String): ProjectSpecificData? {
            return projectsDataFlow.value[name]
        }

        override fun getLive(name: String): StateFlow<ProjectSpecificData?> {
            return projectsDataFlow.map { it[name] }.stateIn(
                CoroutineScope(Dispatchers.Main),
                SharingStarted.Eagerly, null
            )
        }

        override fun list(): List<ProjectSpecificData> {
            return projectsDataFlow.value.values.toList()
        }

        override fun listLive(): StateFlow<List<ProjectSpecificData>> {
            return projectsDataFlow.map { it.values.toList() }.stateIn(
                CoroutineScope(Dispatchers.Main),
                SharingStarted.Eagerly, listOf()
            )
        }
    }

}
