package com.aught.wakawaka.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aught.wakawaka.workers.WakaDataFetchWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.floor


interface WakaDataRepository {
    val projects: ProjectsRepository
    val statistics: StatisticsRepository
    val settings: SettingsRepository
    val cheats: CheatRepository
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

interface ProjectCheatCounts {
    val dailyTotal: Float
    val weeklyTotal: Float
    val dailyUsed: Int
    val weeklyUsed: Int
    val dailyAvailable: Int
        get() = floor(dailyTotal - dailyUsed).toInt()
    val dailyCompletion: Float
        get() = if (dailyTotal == 0f) 0f else ((dailyTotal - dailyUsed.toFloat()) - dailyAvailable.toFloat())
    val weeklyAvailable: Int
        get() = floor(weeklyTotal - weeklyUsed).toInt()
    val weeklyCompletion: Float
        get() = if (weeklyTotal == 0f) 0f else ((weeklyTotal - weeklyUsed.toFloat()) - weeklyAvailable.toFloat())
}

enum class CheatType {
    DAILY,
    WEEKLY
}

interface CheatRepository {
    fun getCheatData(projectName: String? = null): Flow<ProjectCheatData>
    fun saveCheatData(projectCheatData: ProjectCheatData, projectName: String? = null)
    fun getCheatCount(projectName: String? = null): Flow<ProjectCheatCounts?> // project name to project cheat counts
    suspend fun useCheat(
        type: CheatType,
        date: java.time.LocalDate,
        projectName: String? = null
    ): Boolean

    fun getBlockedDates(
        type: CheatType,
        projectName: String? = null
    ): Flow<Set<String>> // dates where target was met or cheat already used

    fun getUsedCheatDates(type: CheatType, projectName: String? = null): Flow<List<String>>
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


    private val _cheatDataFlow =
        MutableStateFlow(WakaDataFetchWorker.loadCheatData(context))
    private val cheatDataFlow: StateFlow<WakaCheatData> = _cheatDataFlow


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

                WakaHelpers.CHEAT_DAY_DATA_KEY -> {
                    val newCheatData = WakaDataFetchWorker.loadCheatData(context)
                    _cheatDataFlow.value = newCheatData
                }
            }
        }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    override val projects: ProjectsRepository = object : ProjectsRepository {
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

    override val statistics: StatisticsRepository = object : StatisticsRepository {
        override fun get(): StateFlow<WakaStatistics> {
            return statisticsFlow
        }
    }

    override val settings: SettingsRepository = object : SettingsRepository {
        override fun get(): StateFlow<SettingsData> {
            return settingsFlow
        }

        override fun save(settingsData: SettingsData) {
            // if daily or weekly target hours were changed, we need to recalculate streaks and save updated aggregate and project data
            val currentSettings = settingsFlow.value
            val dailyTargetChanged = settingsData.dailyTargetHours != currentSettings.dailyTargetHours
            val weeklyTargetChanged = settingsData.weeklyTargetHours != currentSettings.weeklyTargetHours

            if (dailyTargetChanged || weeklyTargetChanged) {
                val wakaDataHandler = WakaDataHandler.fromContext(context)

                // Recalculate and save aggregate data with updated streaks
                val aggregateData = aggregateDataFlow.value
                val updatedAggregateData = aggregateData.copy(
                    dailyTargetHours = settingsData.dailyTargetHours,
                    weeklyTargetHours = settingsData.weeklyTargetHours,
                    dailyStreak = aggregateData.dailyStreak?.copy(
                        count = wakaDataHandler.calculateUpdatedStreak(DataRequest.Aggregate, TimePeriod.DAY),
                        updatedAt = WakaHelpers.ZERO_DAY
                    ),
                    weeklyStreak = aggregateData.weeklyStreak?.copy(
                        count = wakaDataHandler.calculateUpdatedStreak(DataRequest.Aggregate, TimePeriod.WEEK),
                        updatedAt = WakaHelpers.ZERO_DAY
                    )
                )
                WakaDataFetchWorker.saveAggregateData(context, updatedAggregateData)
            }


            WakaDataFetchWorker.saveSettingsData(context, settingsData)
        }
    }

    override val cheats: CheatRepository = object : CheatRepository {
        override fun getCheatData(projectName: String?): Flow<ProjectCheatData> {
            val key = projectName ?: AggregateKey
            return cheatDataFlow.map { cheatData ->
                cheatData.cheatSpecs[key] ?: ProjectCheatData()
            }
        }

        override fun saveCheatData(projectCheatData: ProjectCheatData, projectName: String?) {
            val key = projectName ?: AggregateKey
            val currentCheatData = cheatDataFlow.value
            val updatedCheatSpecs = currentCheatData.cheatSpecs.toMutableMap()
            updatedCheatSpecs[key] = projectCheatData

            val updatedCheatData = WakaCheatData(cheatSpecs = updatedCheatSpecs)
            WakaDataFetchWorker.saveCheatData(context, updatedCheatData)
        }

        override fun getCheatCount(projectName: String?): Flow<ProjectCheatCounts?> {
            val key = projectName ?: AggregateKey
            if (!cheatDataFlow.value.cheatSpecs.containsKey(key)) {
                return MutableStateFlow(null)
            }
            return cheatDataFlow.map { cheatData ->
                val projectCheatData = cheatData.cheatSpecs[key] ?: ProjectCheatData()

                // Get the time range for calculating earned cheats
                val now = java.time.LocalDate.now()
                val dailyWindowStart =
                    now.minusDays(projectCheatData.dailyCheatTimeWindowInDays.toLong())
                val weeklyWindowStart =
                    now.minusDays(projectCheatData.weeklyCheatTimeWindowInDays.toLong())

                // Calculate total hours within the time windows
                val dailyTotalHours = if (projectName == null) {
                    // Aggregate data
                    val aggregateData = aggregateDataFlow.value
                    calculateHoursInRange(
                        aggregateData.dailyRecords.mapValues { it.value.totalSeconds },
                        dailyWindowStart,
                        now
                    )
                } else {
                    // Project-specific data
                    val projectData = projectsDataFlow.value[projectName]
                    if (projectData != null) {
                        calculateHoursInRange(
                            projectData.dailyDurationInSeconds,
                            dailyWindowStart,
                            now
                        )
                    } else {
                        0f
                    }
                }

                val weeklyTotalHours = if (projectName == null) {
                    // Aggregate data
                    val aggregateData = aggregateDataFlow.value
                    calculateHoursInRange(
                        aggregateData.dailyRecords.mapValues { it.value.totalSeconds },
                        weeklyWindowStart,
                        now
                    )
                } else {
                    // Project-specific data
                    val projectData = projectsDataFlow.value[projectName]
                    if (projectData != null) {
                        calculateHoursInRange(
                            projectData.dailyDurationInSeconds,
                            weeklyWindowStart,
                            now
                        )
                    } else {
                        0f
                    }
                }

                // Calculate earned cheats based on hours
                val dailyTotal = dailyTotalHours / projectCheatData.hoursPerCheatDay
                val weeklyTotal = weeklyTotalHours / projectCheatData.hoursPerCheatWeek

                // Count used cheats within time window
                val dailyUsed = projectCheatData.dailyCheatUsageRecord.filter { dateStr ->
                    val date = java.time.LocalDate.parse(dateStr, WakaHelpers.getYYYYMMDDDateFormatter())
                    !date.isBefore(dailyWindowStart)
                }.size
                val weeklyUsed = projectCheatData.weeklyCheatUsageRecord.filter { weekStr ->
                    val weekStartDate = java.time.LocalDate.parse(weekStr, WakaHelpers.getYYYYMMDDDateFormatter())
                    !weekStartDate.isBefore(weeklyWindowStart)
                }.size

                object : ProjectCheatCounts {
                    override val dailyTotal = dailyTotal
                    override val weeklyTotal = weeklyTotal
                    override val dailyUsed = dailyUsed
                    override val weeklyUsed = weeklyUsed
                }
            }
        }

        override suspend fun useCheat(
            type: CheatType,
            date: java.time.LocalDate,
            projectName: String?
        ): Boolean {
            val key = projectName ?: AggregateKey
            val cheatData = cheatDataFlow.value
            val projectCheatData = cheatData.cheatSpecs[key] ?: ProjectCheatData()

            // Calculate available cheats inline
            val now = java.time.LocalDate.now()
            val dailyWindowStart =
                now.minusDays(projectCheatData.dailyCheatTimeWindowInDays.toLong())
            val weeklyWindowStart =
                now.minusDays(projectCheatData.weeklyCheatTimeWindowInDays.toLong())

            // Calculate total hours within the time windows
            val dailyTotalHours = if (projectName == null) {
                val aggregateData = aggregateDataFlow.value
                calculateHoursInRange(
                    aggregateData.dailyRecords.mapValues { it.value.totalSeconds },
                    dailyWindowStart,
                    now
                )
            } else {
                val projectData = projectsDataFlow.value[projectName]
                if (projectData != null) {
                    calculateHoursInRange(
                        projectData.dailyDurationInSeconds,
                        dailyWindowStart,
                        now
                    )
                } else {
                    0f
                }
            }

            val weeklyTotalHours = if (projectName == null) {
                val aggregateData = aggregateDataFlow.value
                calculateHoursInRange(
                    aggregateData.dailyRecords.mapValues { it.value.totalSeconds },
                    weeklyWindowStart,
                    now
                )
            } else {
                val projectData = projectsDataFlow.value[projectName]
                if (projectData != null) {
                    calculateHoursInRange(
                        projectData.dailyDurationInSeconds,
                        weeklyWindowStart,
                        now
                    )
                } else {
                    0f
                }
            }

            val dailyTotal = (dailyTotalHours / projectCheatData.hoursPerCheatDay).toInt()
            val weeklyTotal = (weeklyTotalHours / projectCheatData.hoursPerCheatWeek).toInt()
            val dailyUsed = projectCheatData.dailyCheatUsageRecord.size
            val weeklyUsed = projectCheatData.weeklyCheatUsageRecord.size

            val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()

            return when (type) {
                CheatType.DAILY -> {
                    val available = dailyTotal - dailyUsed
                    if (available <= 0) return false

                    // Add the selected date to the daily usage record
                    val updatedDailyRecord = projectCheatData.dailyCheatUsageRecord.toMutableList()
                    val dateStr = date.format(dateFormatter)
                    if (!updatedDailyRecord.contains(dateStr)) {
                        updatedDailyRecord.add(dateStr)
                    }

                    val updatedProjectCheatData = projectCheatData.copy(
                        dailyCheatUsageRecord = updatedDailyRecord
                    )
                    saveCheatData(updatedProjectCheatData, projectName)

                    // Recalculate and update streaks after using cheat
                    recalculateAndSaveStreaks(projectName)

                    true
                }

                CheatType.WEEKLY -> {
                    val available = weeklyTotal - weeklyUsed
                    if (available <= 0) return false

                    // Add the first day of the selected week to the weekly usage record
                    val firstDayOfWeek =
                        date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    val weekStr = firstDayOfWeek.format(dateFormatter)

                    val updatedWeeklyRecord =
                        projectCheatData.weeklyCheatUsageRecord.toMutableList()
                    if (!updatedWeeklyRecord.contains(weekStr)) {
                        updatedWeeklyRecord.add(weekStr)
                    }

                    val updatedProjectCheatData = projectCheatData.copy(
                        weeklyCheatUsageRecord = updatedWeeklyRecord
                    )
                    saveCheatData(updatedProjectCheatData, projectName)

                    // Recalculate and update streaks after using cheat
                    recalculateAndSaveStreaks(projectName)

                    true
                }
            }
        }

        private fun recalculateAndSaveStreaks(projectName: String?) {
            val wakaDataHandler = WakaDataHandler.fromContext(context)


            if (projectName == null) {
                // Update aggregate streaks
                val aggregateData = aggregateDataFlow.value
                val dataRequest = DataRequest.Aggregate

                val newDailyStreak =
                    wakaDataHandler.calculateUpdatedStreak(dataRequest, TimePeriod.DAY, true)
                val newWeeklyStreak =
                    wakaDataHandler.calculateUpdatedStreak(dataRequest, TimePeriod.WEEK, true)

                val updatedAggregateData = aggregateData.copy(
                    dailyStreak = aggregateData.dailyStreak?.copy(count = newDailyStreak,WakaHelpers.ZERO_DAY),
                    weeklyStreak = aggregateData.weeklyStreak?.copy(count = newWeeklyStreak,WakaHelpers.ZERO_DAY)
                )

                WakaDataFetchWorker.saveAggregateData(context, updatedAggregateData)
            } else {
                // Update project-specific streaks
                val projectData = projectsDataFlow.value[projectName] ?: return
                val dataRequest = DataRequest.ProjectSpecific(projectName)

                val newDailyStreak =
                    wakaDataHandler.calculateUpdatedStreak(dataRequest, TimePeriod.DAY, true)
                val newWeeklyStreak =
                    wakaDataHandler.calculateUpdatedStreak(dataRequest, TimePeriod.WEEK, true)

                val updatedProjectData = projectData.copy(
                    dailyStreak = projectData.dailyStreak?.copy(count = newDailyStreak, WakaHelpers.ZERO_DAY),
                    weeklyStreak = projectData.weeklyStreak?.copy(count = newWeeklyStreak, WakaHelpers.ZERO_DAY)
                )

                WakaDataFetchWorker.saveProjectData(context, projectName, updatedProjectData)
            }
        }

        override fun getBlockedDates(type: CheatType, projectName: String?): Flow<Set<String>> {
            val key = projectName ?: AggregateKey
            return cheatDataFlow.map { cheatData ->
                val projectCheatData = cheatData.cheatSpecs[key] ?: ProjectCheatData()

                // Determine blocked dates based on the cheat type
                when (type) {
                    CheatType.DAILY -> {
                        // Blocked dates are those where the daily target was met or cheat was used
                        projectCheatData.dailyCheatUsageRecord.toSet()
                    }

                    CheatType.WEEKLY -> {
                        // Blocked dates are those where the weekly target was met or cheat was used
                        projectCheatData.weeklyCheatUsageRecord.toSet()
                    }
                }
            }
        }

        override fun getUsedCheatDates(type: CheatType, projectName: String?): Flow<List<String>> {
            val key = projectName ?: AggregateKey
            return cheatDataFlow.map { cheatData ->
                val projectCheatData = cheatData.cheatSpecs[key] ?: ProjectCheatData()

                // Return the list of used cheat dates based on the cheat type
                Log.d("WakaDataRepo", "getUsedCheatDates: $projectCheatData, project name is $projectName")
                when (type) {
                    CheatType.DAILY -> projectCheatData.dailyCheatUsageRecord
                    CheatType.WEEKLY -> projectCheatData.weeklyCheatUsageRecord
                }
            }
        }

        private fun calculateHoursInRange(
            dailyRecords: Map<String, Int>,
            startDate: java.time.LocalDate,
            endDate: java.time.LocalDate
        ): Float {
            val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
            var totalSeconds = 0

            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val dateStr = currentDate.format(dateFormatter)
                totalSeconds += dailyRecords[dateStr] ?: 0
                currentDate = currentDate.plusDays(1)
            }

            return totalSeconds / 3600f
        }
    }


}
