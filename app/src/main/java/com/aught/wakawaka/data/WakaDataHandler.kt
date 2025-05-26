package com.aught.wakawaka.data

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.aught.wakawaka.workers.WakaDataFetchWorker
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.pow
import kotlin.math.sqrt

sealed class DataRequest {
    object Aggregate : DataRequest()
    data class ProjectSpecific(val projectName: String) : DataRequest()
}

enum class TimePeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

class WakaDataHandler(val aggregateData: AggregateData?, val projectSpecificData: Map<String, ProjectSpecificData>) {
    companion object {
        fun fromContext(context: Context): WakaDataHandler {
            val aggregateData = WakaDataFetchWorker.loadAggregateData(context)
            val projectSpecificData = WakaDataFetchWorker.loadProjectSpecificData(context)
            return WakaDataHandler(aggregateData, projectSpecificData)
        }
    }

    fun getDateToDurationData(dataRequest: DataRequest): Map<String, Int> {
//        Log.d("waka", "getDateToDurationData: $dataRequest")
        return when (dataRequest) {
            is DataRequest.Aggregate -> aggregateData?.dailyRecords?.mapValues { it.value.totalSeconds } ?: emptyMap()
            is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.dailyDurationInSeconds ?: emptyMap()
        }
    }

    fun getTarget(dataRequest: DataRequest, period: TimePeriod): Float? {
        return when (period) {
            TimePeriod.DAY -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.dailyTargetHours
                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.dailyTargetHours
                }
            }

            TimePeriod.WEEK -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.weeklyTargetHours
                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.weeklyTargetHours
                }
            }

            TimePeriod.MONTH -> null
            TimePeriod.YEAR -> null
        }
    }

    fun getStreak(dataRequest: DataRequest, period: TimePeriod): StreakData {
        return when (period) {
            TimePeriod.DAY -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.dailyStreak
                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.dailyStreak
                } ?: StreakData(0, WakaHelpers.ZERO_DAY)
            }

            TimePeriod.WEEK -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.weeklyStreak
                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.weeklyStreak
                } ?: StreakData(0, WakaHelpers.ZERO_DAY)
            }

            TimePeriod.MONTH -> StreakData(0, WakaHelpers.ZERO_DAY)
            TimePeriod.YEAR -> StreakData(0, WakaHelpers.ZERO_DAY)
        }
    }

    fun getExcludedDays(dataRequest: DataRequest, period: TimePeriod): Set<Int> {
        return when (period) {
            TimePeriod.DAY -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.excludedDaysFromDailyStreak ?: emptyList()
                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.excludedDaysFromDailyStreak ?: emptyList()
                }
            }

            TimePeriod.WEEK -> emptyList()
            TimePeriod.MONTH -> emptyList()
            TimePeriod.YEAR -> emptyList()
        }.toSet()
    }

    fun getLastXDaysDurationInSeconds(dataRequest: DataRequest, days: Int): Int {
        val data = getDateToDurationData(dataRequest)
        val today = LocalDate.now()

        var total = 0;
        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            total += data[date.toString()] ?: 0
        }

        return total
    }

    fun getPeriodicDateAtOffset(dataRequest: DataRequest, period: TimePeriod, offset: Int): LocalDate {
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

    /**
     * Get the duration in seconds for the offset-th time period before the current time period.
     * e.g if offset is 0, it returns the current time period
     * e.g if offset is 1, it returns the previous time period
     * e.g if offset is 2, it returns the time period before the previous time period
     */
    fun getOffsetPeriodicDurationInSeconds(dataRequest: DataRequest, period: TimePeriod, offset: Int): Int {
        if (offset < 0) {
            throw IllegalArgumentException("Offset must be greater than or equal to 0")
        }
        val data = getDateToDurationData(dataRequest)
        val today = LocalDate.now()
        return when (period) {
            TimePeriod.DAY -> {
                val date = today.minusDays(offset.toLong())
                data[date.toString()] ?: 0
            }

            TimePeriod.WEEK -> {
                // get the first day of this week (Monday)
                val firstDayOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val date = firstDayOfWeek.minusWeeks(offset.toLong())
                (0..6).sumOf { j ->
                    val day = date.plusDays(j.toLong())
                    data[day.toString()] ?: 0
                }
            }

            TimePeriod.MONTH -> {
                // get the first day of this month
                val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
                val date = firstDayOfMonth.minusMonths(offset.toLong())
                (0..date.lengthOfMonth()).sumOf { j ->
                    val day = date.plusDays(j.toLong())
                    data[day.toString()] ?: 0
                }
            }

            TimePeriod.YEAR -> {
                val firstDayOfYear = today.with(TemporalAdjusters.firstDayOfYear())
                val date = firstDayOfYear.minusYears(offset.toLong())
                (0..date.lengthOfYear()).sumOf { j ->
                    val day = date.plusDays(j.toLong())
                    data[day.toString()] ?: 0
                }
            }
        }
    }

    /**
     * Get the duration in seconds for the last X reps of the given time period as a list of length X from earliest to latest.
     * TimePeriod.Day returns the last X days
     * TimePeriod.Week returns the last X weeks (Monday to Sunday) with this week included
     * TimePeriod.Month returns the last X months (1st to end of month) with this month included
     * TimePeriod.Year returns the last X years (1st Jan to end of year) with this year included
     */
    fun getPeriodicDurationsInSeconds(dataRequest: DataRequest, period: TimePeriod, reps: Int = 1): List<Int> {
        return (0..reps - 1).reversed().map { i ->
            getOffsetPeriodicDurationInSeconds(dataRequest, period, i)
        }
    }

    fun targetHit(dataRequest: DataRequest, period: TimePeriod): Boolean {
        return when (period) {
            TimePeriod.DAY -> {
                val target = when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.dailyTargetHours
                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.dailyTargetHours
                }
                val duration = getPeriodicDurationsInSeconds(dataRequest, period)[0]
                if (target == null) {
                    return duration > 0
                }
                return duration >= target * 3600
            }

            TimePeriod.WEEK -> {
                val target = when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.weeklyTargetHours
                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.weeklyTargetHours
                }
                val duration = getPeriodicDurationsInSeconds(dataRequest, period)[0]

                if (target == null) {
                    return duration > 0
                }
                return duration >= target * 3600
            }

            TimePeriod.MONTH -> {
                // maybe there'll be a monthly target one day
//                val target = null
                val duration = getPeriodicDurationsInSeconds(dataRequest, period)[0]
                return duration > 0

            }

            TimePeriod.YEAR -> {
                // maybe there'll be a yearly target one day
//                val target = null
                val duration = getPeriodicDurationsInSeconds(dataRequest, period)[0]
                return duration > 0
            }
        }
    }

    fun calculateUpdatedStreak(dataRequest: DataRequest, period: TimePeriod): Int {
        var streak = 0
        var offset = 0

        val target = getTarget(dataRequest, period)
        val currentStreak = getStreak(dataRequest, period)
        val excludedDays = getExcludedDays(dataRequest, period)


        while (true) {
            offset++
            val date = getPeriodicDateAtOffset(dataRequest, period, offset)
            if (date.toString() == currentStreak.updatedAt) {
                streak += currentStreak.count
                break
            }
            val duration = getOffsetPeriodicDurationInSeconds(dataRequest, period, offset)
            if (excludedDays.contains(date.dayOfWeek.value)) {
                continue
            }
            if (target == null) {
                if (duration == 0) break
            } else {
                if (duration < target * 3600) break
            }
            streak++
        }
        return streak
    }

    // region PROJECT SPECIFIC

    fun getProjectColor(projectName: String): Color {
        val projectData = projectSpecificData[projectName]
        if (projectData == null) {
            return WakaHelpers.projectNameToColor(projectName)
        }
        return runCatching {
            Color(projectData.color.toColorInt())
        }.getOrNull() ?: WakaHelpers.projectNameToColor(projectData.name)
    }

    // get the sorted project list based on the duration over the last 30 days
    fun getSortedProjectList(): List<String> {
        val sortedProjectList = projectSpecificData.toList().sortedByDescending { (projectName, _) ->
            val data = getDateToDurationData(DataRequest.ProjectSpecific(projectName))
            // sum up the durations weighted by the square root of the reciprocal of the number of days ago it happened
            data.entries.sumOf { (date, duration) ->
                val daysAgo = LocalDate.now().toEpochDay() - LocalDate.parse(date).toEpochDay()
                if (daysAgo == 0L) {
                    duration // today's data is not weighted
                } else {
                    duration / (daysAgo.toDouble()).toInt()
                }
            }
        }.map { it.first }

        return sortedProjectList
    }

    // endregion
}
