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

class WakaDataTransformer(wakaDataRepository: WakaDataRepository) {
    private val p = wakaDataRepository.projects
    private val s = wakaDataRepository.statistics

    // helpers
    companion object {
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

    }

    fun getAggregate(): Flow<AggregateData> {
        return p.getAggregate()
    }

    fun getProjects(sorted: Boolean = false): Flow<List<ProjectSpecificData>> {
        return if (sorted) p.list() else {
            p.list().map {
                it.sortedByDescending { data ->
                    // sum up the durations weighted by the square root of the reciprocal of the number of days ago it happened
                    data.dailyDurationInSeconds.entries.sumOf { (date, duration) ->
                        val daysAgo = LocalDate.now().toEpochDay() - LocalDate.parse(date).toEpochDay()
                        if (daysAgo == 0L) {
                            duration // today's data is not weighted
                        } else {
                            duration / (daysAgo.toDouble()).toInt()
                        }
                    }
                }
            }
        }
    }

    fun getProject(projectName: String): Flow<ProjectSpecificData?> {
        return p.get(projectName)
    }

    fun getDateToDurationData(dataRequest: DataRequest): Flow<Map<String, Int>> {
        return when (dataRequest) {
            is DataRequest.Aggregate -> p.getAggregate().map {
                it.dailyRecords.mapValues { it.value.totalSeconds }
            }

            is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map {
                it?.dailyDurationInSeconds ?: emptyMap()
            }
        }
    }

    fun getLabelledDurationStats(dataRequest: DataRequest): Flow<Map<String, Int>> {
        return combine(
            s.get(),
            getOffsetPeriodicDurationInSeconds(dataRequest, TimePeriod.WEEK, 0)
        ) { it, thisWeekSeconds ->
            val stats = when (dataRequest) {
                is DataRequest.Aggregate -> it.aggregateStats
                is DataRequest.ProjectSpecific -> it.projectStats[dataRequest.projectName] ?: DurationStats()
            }

            mapOf(
                "Today" to stats.today,
                "This Week" to thisWeekSeconds,
//        "Last 7 Days" to durationStats.last7Days,
                "Past 30 Days" to stats.last30Days,
                "Past Year" to stats.lastYear,
                "All Time" to stats.allTime
            )
        }
    }

    // region TARGET STREAK DATA
    // ? ........................

    fun getTarget(dataRequest: DataRequest, period: TimePeriod): Flow<Float?> {
        return when (period) {
            TimePeriod.DAY -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> p.getAggregate().map { it.dailyTargetHours }
                    is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.dailyTargetHours }
                }
            }

            TimePeriod.WEEK -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> p.getAggregate().map { it.weeklyTargetHours }
                    is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.weeklyTargetHours }
                }
            }

            TimePeriod.MONTH -> flowOf(null)
            TimePeriod.YEAR -> flowOf(null)
        }
    }

    fun getStreak(dataRequest: DataRequest, period: TimePeriod): Flow<StreakData> {
        val defaultStreak = StreakData(0, WakaHelpers.ZERO_DAY)
        return when (period) {
            TimePeriod.DAY -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> p.getAggregate().map { it.dailyStreak }
                    is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.dailyStreak }
                }
            }

            TimePeriod.WEEK -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> p.getAggregate().map { it.weeklyStreak }
                    is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName).map { it?.weeklyStreak }
                }
            }

            TimePeriod.MONTH -> flowOf(null)
            TimePeriod.YEAR -> flowOf(null)
        }.map { it ?: defaultStreak }
    }

    fun getStreakCompletion(dataRequest: DataRequest, period: TimePeriod): Flow<Float> {
        // We use combine to ensure this calculation runs whenever either the target or duration changes.
        return combine(
            getTarget(dataRequest, period),
            getPeriodicDurationsInSeconds(dataRequest, period, 1)
        ) { target, durations ->
            val duration = durations.getOrElse(0) { 0 }
            Log.d("HomeViewModel", "Calculating streak completion: target=$target, duration=$duration")
            if (target == null) {
                return@combine if (duration > 0) 1f else 0f
            }
            (duration.toFloat() / (target * 3600)).coerceIn(0f, 1f)
        }
    }

    fun targetHit(dataRequest: DataRequest, period: TimePeriod): Flow<Boolean> {
        return combine(
            getTarget(dataRequest, period),
            getPeriodicDurationsInSeconds(dataRequest, period, 1)
        ) { target, durations ->
            val duration = durations.getOrElse(0) { 0 }
            if (target == null) {
                duration > 0
            } else {
                duration >= target * 3600
            }
        }
    }

    fun getTargetStreak(dataRequest: DataRequest, period: TimePeriod): Flow<TargetStreakData> {
        return combine(
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

    // ? ........................
    // endregion ........................


    // region TIMING AND DATE DURATION DATA
    // ? ........................

    fun getLastXDaysDurationInSeconds(dataRequest: DataRequest, days: Int): Flow<Int> {
        return getDateToDurationData(dataRequest)
            .map { data ->
                val today = LocalDate.now()
                (0 until days).sumOf { i ->
                    val date = today.minusDays(i.toLong())
                    data[date.toString()] ?: 0
                }
            }
    }

    // You can keep the remaining functions as non-Flow-returning private helpers
    // within the Use Case if they are only used for intermediate calculations.
    private fun getPeriodicDateAtOffset(dataRequest: DataRequest, period: TimePeriod, offset: Int): LocalDate {
        if (offset < 0) {
            throw IllegalArgumentException("Offset must be greater than or equal to 0")
        }
        val today = LocalDate.now()
        return when (period) {
            TimePeriod.DAY -> today.minusDays(offset.toLong())
            TimePeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset.toLong())
            TimePeriod.MONTH -> today.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(offset.toLong())
            TimePeriod.YEAR -> today.with(TemporalAdjusters.firstDayOfYear()).minusYears(offset.toLong())
        }
    }

    fun getPeriodicDates(dataRequest: DataRequest, period: TimePeriod, reps: Int = 1): List<LocalDate> {
        return (0..reps - 1).reversed().map { i ->
            getPeriodicDateAtOffset(dataRequest, period, i)
        }
    }


    fun getOffsetPeriodicDurationInSeconds(dataRequest: DataRequest, period: TimePeriod, offset: Int): Flow<Int> {
        return getDateToDurationData(dataRequest).map { data ->
            calcOffsetPeriodicDurationInSeconds(data, period, offset)
        }
    }

    fun getPeriodicDurationsInSeconds(dataRequest: DataRequest, period: TimePeriod, reps: Int = 1): Flow<List<Int>> {
        return getDateToDurationData(dataRequest).map { data ->
            (0..reps - 1).reversed().map { i ->
                calcOffsetPeriodicDurationInSeconds(data, period, i)
            }
        }
    }

    // ? ........................
    // endregion ........................

    fun getExcludedDays(dataRequest: DataRequest, period: TimePeriod): Flow<Set<Int>> {
        return when (period) {
            TimePeriod.DAY -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> p.getAggregate()
                        .map { it.excludedDaysFromDailyStreak.toSet() }

                    is DataRequest.ProjectSpecific -> p.get(dataRequest.projectName)
                        .map { it?.excludedDaysFromDailyStreak?.toSet() ?: emptySet() }
                }
            }

            else -> flowOf(emptySet())
        }
    }


    fun getProjectColor(dataRequest: DataRequest): Flow<Color?> {
        Log.d("HomeViewModel", "Getting project color for request: $dataRequest")
        return when (dataRequest) {
            // Case 1: Aggregate data is always null
            is DataRequest.Aggregate -> {
                flowOf(null)
            }
            // Case 2: Project-specific data needs to be fetched
            is DataRequest.ProjectSpecific -> {
                p.get(dataRequest.projectName) // Assuming this returns Flow<Project?>
                    .map { project ->
                        Log.d("HomeViewModel", "Fetched project: $project")
                        val color = if (project?.color != null) {
                            // Safely parse color
                            runCatching { Color(project.color.toColorInt()) }.getOrNull()
                                ?: WakaHelpers.projectNameToColor(project.name)
                        } else {
                            WakaHelpers.projectNameToColor(project?.name)
                        }
                        // Wrap the final result in Success
                        color
                    }
                    // Use onStart to emit the Loading state *before* the repository emits its first value
            }
        }
    }
}
