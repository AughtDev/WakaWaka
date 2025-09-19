package com.aught.wakawaka.data

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.map

data class TargetStreakData(
    val target: Float?,
    val streak: Int,
    val completion: Float,
    val targetHit: Boolean
)

sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
}

object WakaDataTransformers {
    fun calcOffsetPeriodicDurationInSeconds(data: Map<String, Int>, period: TimePeriod, offset: Int): Int {
        if (offset < 0) {
            throw IllegalArgumentException("Offset must be greater than or equal to 0")
        }
        val today = LocalDate.now()
        return when (period) {
            TimePeriod.DAY -> {
                val date = today.minusDays(offset.toLong())
                data[date.toString()] ?: 0
            }

            TimePeriod.WEEK -> {
                val firstDayOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val date = firstDayOfWeek.minusWeeks(offset.toLong())
                (0..6).sumOf { j ->
                    val day = date.plusDays(j.toLong())
                    data[day.toString()] ?: 0
                }
            }

            TimePeriod.MONTH -> {
                val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
                val date = firstDayOfMonth.minusMonths(offset.toLong())
                (0..date.lengthOfMonth() - 1).sumOf { j ->
                    val day = date.plusDays(j.toLong())
                    data[day.toString()] ?: 0
                }
            }

            TimePeriod.YEAR -> {
                val firstDayOfYear = today.with(TemporalAdjusters.firstDayOfYear())
                val date = firstDayOfYear.minusYears(offset.toLong())
                (0..date.lengthOfYear() - 1).sumOf { j ->
                    val day = date.plusDays(j.toLong())
                    data[day.toString()] ?: 0
                }
            }
        }
    }

    fun calcPeriodicDurationsInSeconds(data: Map<String, Int>, period: TimePeriod,reps: Int = 1): List<Int> {
        return (0..reps - 1).reversed().map { i ->
            calcOffsetPeriodicDurationInSeconds(data, period, i)
        }
    }

    fun calcLabelledDurationStats(dataRequest: DataRequest,stats: WakaStatistics,thisWeekDuration: Int): Map<String,Int> {
        val relevantStats = when (dataRequest) {
            is DataRequest.Aggregate -> stats.aggregateStats
            is DataRequest.ProjectSpecific -> stats.projectStats[dataRequest.projectName] ?: DurationStats()
        }
        return mapOf(
            "Today" to relevantStats.today,
            "This Week" to thisWeekDuration,
            "Past 30 Days" to relevantStats.last30Days,
            "Past Year" to relevantStats.lastYear,
            "All Time" to relevantStats.allTime
        )
    }

    // region AGGREGATE DATA
    // ? ........................

    fun getAggregateTarget(aggregateData: AggregateData, period: TimePeriod): Float? {
        return when (period) {
            TimePeriod.DAY -> aggregateData.dailyTargetHours
            TimePeriod.WEEK -> aggregateData.weeklyTargetHours
            TimePeriod.MONTH, TimePeriod.YEAR -> null
        }
    }

    fun getAggregateStreak(aggregateData: AggregateData, period: TimePeriod): StreakData {
        return when (period) {
            TimePeriod.DAY -> aggregateData.dailyStreak
            TimePeriod.WEEK -> aggregateData.weeklyStreak
            TimePeriod.MONTH, TimePeriod.YEAR -> StreakData(0, WakaHelpers.ZERO_DAY)
        } ?: StreakData(0, WakaHelpers.ZERO_DAY)
    }

    fun getAggregateExcludedDays(aggregateData: AggregateData, period: TimePeriod): Set<Int> {
        return when (period) {
            TimePeriod.DAY -> aggregateData.excludedDaysFromDailyStreak.toSet()
            TimePeriod.WEEK, TimePeriod.MONTH, TimePeriod.YEAR -> emptySet()
        }
    }

    // ? ........................
    // endregion ........................


    // region PROJECT DATA
    // ? ........................

    fun getProjectTarget(projectData: ProjectSpecificData?, period: TimePeriod): Float? {
        return when (period) {
            TimePeriod.DAY -> projectData?.dailyTargetHours
            TimePeriod.WEEK -> projectData?.weeklyTargetHours
            TimePeriod.MONTH, TimePeriod.YEAR -> null
        }
    }

    fun getProjectStreak(projectData: ProjectSpecificData?, period: TimePeriod): StreakData {
        return when (period) {
            TimePeriod.DAY -> projectData?.dailyStreak
            TimePeriod.WEEK -> projectData?.weeklyStreak
            TimePeriod.MONTH, TimePeriod.YEAR -> StreakData(0, WakaHelpers.ZERO_DAY)
        } ?: StreakData(0, WakaHelpers.ZERO_DAY)
    }

    fun getProjectExcludedDays(projectData: ProjectSpecificData?, period: TimePeriod): Set<Int> {
        return when (period) {
            TimePeriod.DAY -> projectData?.excludedDaysFromDailyStreak?.toSet() ?: emptySet()
            TimePeriod.WEEK, TimePeriod.MONTH, TimePeriod.YEAR -> emptySet()
        }
    }
    fun getProjectColor(projectData: ProjectSpecificData?): Color? {
        return if (projectData?.color != null) {
            runCatching { Color(projectData.color.toColorInt()) }.getOrNull()
                ?: WakaHelpers.projectNameToColor(projectData.name)
        } else {
            WakaHelpers.projectNameToColor(null)
        }
    }

    // ? ........................
    // endregion ........................


    fun calcCompletion(target: Float?, duration: Int): Float {
        return if (target == null) {
            if (duration > 0) 1f else 0f
        } else {
            (duration.toFloat() / (target * 3600)).coerceIn(0f, 1f)
        }
    }

    fun calcLastXDaysDurationInSeconds(data: Map<String, Int>, days: Int): Int {
        val today = LocalDate.now()
        return (0 until days).sumOf { i ->
            val date = today.minusDays(i.toLong())
            data[date.toString()] ?: 0
        }
    }


}

class WakaDataUseCase(wakaDataRepository: WakaDataRepository) {
    private val p = wakaDataRepository.projects
    private val s = wakaDataRepository.statistics

    // helpers

    // region DATA STATE HELPERS

    private fun <T, R> Flow<T>.toDataState(mapper: (T) -> R): Flow<DataState<R>> =
        this.map { DataState.Success(mapper(it)) as DataState<R> }
            .onStart { emit(DataState.Loading) }

    private fun <T> Flow<T>.toDataState(): Flow<DataState<T>> =
        this.map { DataState.Success(it) as DataState<T> }
            .onStart { emit(DataState.Loading) }

    private fun <A, B, R> combineDataState(
        fa: Flow<DataState<A>>,
        fb: Flow<DataState<B>>,
        block: suspend (A, B) -> R
    ): Flow<DataState<R>> = combine(fa, fb) { a, b ->
        if (a is DataState.Success && b is DataState.Success) {
            DataState.Success(block(a.data, b.data))
        } else {
            DataState.Loading
        }
    }

    private fun <A, B, C, R> combineDataState(
        fa: Flow<DataState<A>>,
        fb: Flow<DataState<B>>,
        fc: Flow<DataState<C>>,
        block: suspend (A, B, C) -> R
    ): Flow<DataState<R>> = combine(fa, fb, fc) { a, b, c ->
        if (a is DataState.Success && b is DataState.Success && c is DataState.Success) {
            DataState.Success(block(a.data, b.data, c.data))
        } else {
            DataState.Loading
        }
    }

    private fun <A, B, C, D, R> combineDataState(
        fa: Flow<DataState<A>>,
        fb: Flow<DataState<B>>,
        fc: Flow<DataState<C>>,
        fd: Flow<DataState<D>>,
        block: suspend (A, B, C, D) -> R
    ): Flow<DataState<R>> = combine(fa, fb, fc, fd) { a, b, c, d ->
        if (a is DataState.Success && b is DataState.Success && c is DataState.Success && d is DataState.Success) {
            DataState.Success(block(a.data, b.data, c.data, d.data))
        } else {
            DataState.Loading
        }
    }

    // endregion

    // region RAW DATA
    // ? ........................

    fun getAggregate(): Flow<AggregateData> {
        return p.getAggregate()
    }

    fun getProjects(sorted: Boolean = false): Flow<List<ProjectSpecificData>> {
        return if (sorted) {
            p.list()
        } else {
            p.list().map {
                it.sortedByDescending { data ->
                    data.dailyDurationInSeconds.entries.sumOf { (date, duration) ->
                        val daysAgo = LocalDate.now().toEpochDay() - LocalDate.parse(date).toEpochDay()
                        if (daysAgo == 0L) duration else duration / (daysAgo.toDouble()).toInt()
                    }
                }
            }
        }
    }

    fun getProject(projectName: String): Flow<DataState<ProjectSpecificData?>> {
        return p.get(projectName).toDataState()
    }

    // ? ........................
    // endregion ........................


    fun getDateToDurationData(dataRequest: DataRequest): Flow<DataState<Map<String, Int>>> {
        return when (dataRequest) {
            is DataRequest.Aggregate -> p.getAggregate().map {
                it.dailyRecords.mapValues { it.value.totalSeconds }
            }.toDataState()
            is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map {
                it?.dailyDurationInSeconds ?: emptyMap()
            }.toDataState()
        }
    }

    fun getLabelledDurationStats(dataRequest: DataRequest): Flow<DataState<Map<String, Int>>> {
        return combineDataState(
            s.get().toDataState(),
            getOffsetPeriodicDurationInSeconds(dataRequest, TimePeriod.WEEK, 0)
        ) { statsState, thisWeekState ->
            WakaDataTransformers.calcLabelledDurationStats(dataRequest, statsState, thisWeekState)
//            val stats = when (dataRequest) {
//                is DataRequest.Aggregate -> statsState.aggregateStats
//                is DataRequest.ProjectSpecific -> statsState.projectStats[dataRequest.projectName] ?: DurationStats()
//            }
//            mapOf(
//                "Today" to stats.today,
//                "This Week" to thisWeekState,
//                "Past 30 Days" to stats.last30Days,
//                "Past Year" to stats.lastYear,
//                "All Time" to stats.allTime
//            )
        }
    }

    fun getTarget(dataRequest: DataRequest, period: TimePeriod): Flow<DataState<Float?>> {
        return when (dataRequest) {
            is DataRequest.Aggregate -> p.getAggregate().map {
                WakaDataTransformers.getAggregateTarget(it, period)
            }.toDataState()
            is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map {
                WakaDataTransformers.getProjectTarget(it, period)
            }.toDataState()
        }
//        return when (period) {
//            TimePeriod.DAY -> when (dataRequest) {
//                is DataRequest.Aggregate -> p.getAggregate().map { it.dailyTargetHours }.toDataState()
//                is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.dailyTargetHours }.toDataState()
//            }
//            TimePeriod.WEEK -> when (dataRequest) {
//                is DataRequest.Aggregate -> p.getAggregate().map { it.weeklyTargetHours }.toDataState()
//                is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.weeklyTargetHours }.toDataState()
//            }
//            TimePeriod.MONTH, TimePeriod.YEAR -> flowOf(DataState.Success(null))
//        }
    }

    fun getStreak(dataRequest: DataRequest, period: TimePeriod): Flow<DataState<StreakData>> {
//        val defaultStreak = StreakData(0, WakaHelpers.ZERO_DAY)
//        val baseFlow: Flow<StreakData?> = when (period) {
//            TimePeriod.DAY -> when (dataRequest) {
//                is DataRequest.Aggregate -> p.getAggregate().map { it.dailyStreak }
//                is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.dailyStreak }
//            }
//            TimePeriod.WEEK -> when (dataRequest) {
//                is DataRequest.Aggregate -> p.getAggregate().map { it.weeklyStreak }
//                is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.weeklyStreak }
//            }
//            TimePeriod.MONTH, TimePeriod.YEAR -> flowOf(null)
//        }
//        return baseFlow.map { it ?: defaultStreak }.toDataState()
        return when (dataRequest) {
            is DataRequest.Aggregate -> p.getAggregate().map {
                WakaDataTransformers.getAggregateStreak(it, period)
            }.toDataState()
            is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map {
                WakaDataTransformers.getProjectStreak(it, period)
            }.toDataState()
        }

    }

    fun getStreakCompletion(dataRequest: DataRequest, period: TimePeriod): Flow<DataState<Float>> {
        return combineDataState(
            getTarget(dataRequest, period),
            getOffsetPeriodicDurationInSeconds(dataRequest, period, 0)
        ) { target, duration ->
            WakaDataTransformers.calcCompletion(target, duration)
        }
    }

    fun targetHit(dataRequest: DataRequest, period: TimePeriod): Flow<DataState<Boolean>> {
        return combineDataState(
            getTarget(dataRequest, period),
            getOffsetPeriodicDurationInSeconds(dataRequest, period, 0)
        ) { target, duration ->
            if (target == null) duration > 0 else duration >= target * 3600
        }
    }

    fun getTargetStreak(dataRequest: DataRequest, period: TimePeriod): Flow<DataState<TargetStreakData>> {
        return combineDataState(
            getTarget(dataRequest, period),
            getStreak(dataRequest, period),
            getStreakCompletion(dataRequest, period),
            targetHit(dataRequest, period)
        ) { target, streak, completion, targetHit ->
            TargetStreakData(
                target = target,
                streak = streak.count + if (targetHit) 1 else 0,
                completion = completion,
                targetHit = targetHit
            )
        }
    }

    fun getLastXDaysDurationInSeconds(dataRequest: DataRequest, days: Int): Flow<DataState<Int>> {
        return getDateToDurationData(dataRequest).map { state ->
            when (state) {
                is DataState.Success -> {
                    WakaDataTransformers.calcLastXDaysDurationInSeconds(state.data, days)
                }
                else -> null
            }
        }.map {
            if (it == null) DataState.Loading else DataState.Success(it)
        }.onStart { emit(DataState.Loading) }
    }

    fun getOffsetPeriodicDurationInSeconds(dataRequest: DataRequest, period: TimePeriod, offset: Int): Flow<DataState<Int>> {
        return getDateToDurationData(dataRequest).map { state ->
            when (state) {
                is DataState.Success -> DataState.Success(WakaDataTransformers.calcOffsetPeriodicDurationInSeconds(state.data, period, offset))
                else -> DataState.Loading
            }
        }.onStart { emit(DataState.Loading) }
    }

    fun getPeriodicDurationsInSeconds(dataRequest: DataRequest, period: TimePeriod, reps: Int = 1): Flow<DataState<List<Int>>> {
        return getDateToDurationData(dataRequest).map { state ->
            when (state) {
                is DataState.Success -> DataState.Success(
                    (0..reps - 1).reversed().map { i ->
                        WakaDataTransformers.calcOffsetPeriodicDurationInSeconds(state.data, period, i)
                    }
                )
                else -> DataState.Loading
            }
        }.onStart { emit(DataState.Loading) }
    }

    fun getExcludedDays(dataRequest: DataRequest, period: TimePeriod): Flow<DataState<Set<Int>>> {
        return when (period) {
            TimePeriod.DAY -> when (dataRequest) {
                is DataRequest.Aggregate -> p.getAggregate()
                    .map { WakaDataTransformers.getAggregateExcludedDays(it, period) }
                    .toDataState()
                is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName)
                    .map { WakaDataTransformers.getProjectExcludedDays(it, period) }
                    .toDataState()
            }
            else -> flowOf(DataState.Success(emptySet()))
        }
    }

    fun getProjectColor(dataRequest: DataRequest): Flow<DataState<Color?>> {
        Log.d("HomeViewModel", "Getting project color for request: $dataRequest")
        return when (dataRequest) {
            is DataRequest.Aggregate -> flowOf(null)
            is DataRequest.ProjectSpecific -> {
                p.get(dataRequest.projectName)
                    .map { project -> WakaDataTransformers.getProjectColor(project) }
            }
        }.toDataState()
    }
}
