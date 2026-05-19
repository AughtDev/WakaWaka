package com.aught.wakawaka.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.aught.wakawaka.workers.WakaDataFetchWorker
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

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


data class ProjectTargetCompletionData(
    val name: String,
    val color: String,
    val completion: Float,
)

class WakaDataHandler(
    val aggregateData: AggregateData?,
    val projectSpecificData: Map<String, ProjectSpecificData>,
    val cheatData: WakaCheatData? = null
) {
    companion object {
        fun fromContext(context: Context): WakaDataHandler {
            val aggregateData = WakaDataFetchWorker.loadAggregateData(context)
            val projectSpecificData = WakaDataFetchWorker.loadProjectSpecificData(context)
            val cheatData = WakaDataFetchWorker.loadCheatData(context)
            return WakaDataHandler(aggregateData, projectSpecificData, cheatData)
        }


    }

    fun getDateToDurationData(dataRequest: DataRequest): Map<String, Int> {
//        Log.d("waka", "getDateToDurationData: $dataRequest")
        return when (dataRequest) {
            is DataRequest.Aggregate -> aggregateData?.dailyRecords?.mapValues { it.value.totalSeconds }
                ?: emptyMap()

            is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.dailyDurationInSeconds
                ?: emptyMap()
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

    fun getStreakCompletion(dataRequest: DataRequest, period: TimePeriod): Float {
        val target = getTarget(dataRequest, period)
        val duration = getPeriodicDurationsInSeconds(dataRequest, period)[0]
        if (target == null) {
            return if (duration > 0) 1f else 0f
        }
        return (duration.toFloat() / (target * 3600)).coerceIn(0f, 1f)
    }

    fun getExcludedDays(dataRequest: DataRequest, period: TimePeriod): Set<Int> {
        return when (period) {
            TimePeriod.DAY -> {
                when (dataRequest) {
                    is DataRequest.Aggregate -> aggregateData?.excludedDaysFromDailyStreak
                        ?: emptyList()

                    is DataRequest.ProjectSpecific -> projectSpecificData[dataRequest.projectName]?.excludedDaysFromDailyStreak
                        ?: emptyList()
                }
            }

            TimePeriod.WEEK -> emptyList()
            TimePeriod.MONTH -> emptyList()
            TimePeriod.YEAR -> emptyList()
        }.toSet()
    }

    fun getCheatDays(dataRequest: DataRequest, period: TimePeriod): Set<String> {
        val key = when (dataRequest) {
            is DataRequest.Aggregate -> AggregateKey
            is DataRequest.ProjectSpecific -> dataRequest.projectName
        }
        val projectCheatData = cheatData?.cheatSpecs?.get(key) ?: return emptySet()

        return when (period) {
            TimePeriod.DAY -> projectCheatData.dailyCheatUsageRecord.toSet()
            TimePeriod.WEEK -> projectCheatData.weeklyCheatUsageRecord.toSet()
            TimePeriod.MONTH -> emptySet()
            TimePeriod.YEAR -> emptySet()
        }
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


    /**
     * Get the duration in seconds for the offset-th time period before the current time period.
     * e.g if offset is 0, it returns the current time period
     * e.g if offset is 1, it returns the previous time period
     * e.g if offset is 2, it returns the time period before the previous time period
     */
    fun getOffsetPeriodicDurationInSeconds(
        dataRequest: DataRequest,
        period: TimePeriod,
        offset: Int
    ): Int {
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
    fun getPeriodicDurationsInSeconds(
        dataRequest: DataRequest,
        period: TimePeriod,
        reps: Int = 1
    ): List<Int> {
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

    fun calculateUpdatedStreak(dataRequest: DataRequest, period: TimePeriod, fromScratch: Boolean = false): Int {
        var streak = 0
        var offset = 0

        val target = getTarget(dataRequest, period)
        val currentStreak = getStreak(dataRequest, period)
        val excludedDays = getExcludedDays(dataRequest, period)
        val cheatDays = getCheatDays(dataRequest, period)
        val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()

        while (true) {
            offset++
            val date = getPeriodicDateAtOffset(period, offset)
            if (date.toString() == currentStreak.updatedAt && !fromScratch) {
                streak += currentStreak.count
                break
            }
            val duration = getOffsetPeriodicDurationInSeconds(dataRequest, period, offset)
            // Skip excluded days (by day of week)
            if (excludedDays.contains(date.dayOfWeek.value)) {
                continue
            }
            // Skip cheat days (by specific date)
            val formattedDate = date.format(dateFormatter)
            if (cheatDays.contains(formattedDate)) {
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
    private fun generateSortedProjectsList(): List<String> {
        val sortedProjectList =
            projectSpecificData.toList().sortedByDescending { (projectName, _) ->
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

    val sortedProjectList by lazy {
        generateSortedProjectsList()
    }


    // region COMPLETION TIER

    /**
     * The HHMM clock time at which the day's cumulative seconds first reached the daily target.
     *
     * Returns:
     *  - `null` when the day has no progress data at all (so the caller can decide: skip from
     *    averages, render the bar as plain white/gray);
     *  - `Int.MAX_VALUE` when progress data exists but the target was never hit;
     *  - otherwise the smallest HHMM key in the progress map whose cumulative seconds met the target.
     *
     * For aggregate data the progress map lives on [DailyAggregateData.progress]. Project-specific
     * data does not currently track intraday progress, so this returns null for project requests.
     */
    fun getCompletionHHMM(dataRequest: DataRequest, date: LocalDate): Int? {
        val formattedDate = date.toString()
        val progress: Map<String, Int> = when (dataRequest) {
            is DataRequest.Aggregate ->
                aggregateData?.dailyRecords?.get(formattedDate)?.progress ?: return null
            is DataRequest.ProjectSpecific -> return null
        }
        if (progress.isEmpty()) return null

        val targetHours = getTarget(dataRequest, TimePeriod.DAY)
        val targetSeconds: Int = if (targetHours == null) 1 else (targetHours * 3600f).toInt()

        val firstHit = progress.entries
            .mapNotNull { e -> e.key.toIntOrNull()?.let { it to e.value } }
            .sortedBy { it.first }
            .firstOrNull { it.second >= targetSeconds }
            ?.first

        return firstHit ?: Int.MAX_VALUE
    }

    /**
     * Tier for the given day's completion time. Returns null when there is no progress data,
     * [CompletionTier.None] when the target was never hit or was hit only after the last cutoff.
     */
    fun getCompletionTier(dataRequest: DataRequest, date: LocalDate): CompletionTier? {
        val hhmm = getCompletionHHMM(dataRequest, date) ?: return null
        if (hhmm == Int.MAX_VALUE) return CompletionTier.None
        return CompletionTierConfig.tierForHHMM(hhmm)
    }

    /**
     * Average completion tier across the past [days] days (excluding today).
     *
     *  - Days with no progress data are skipped.
     *  - Cheat days and excluded-weekday days for the daily streak are skipped.
     *  - Days where data exists but the target was missed count as 24:00 (end-of-day).
     *
     * Returns null when no days were eligible.
     */
    fun getAverageCompletionTier(
        dataRequest: DataRequest = DataRequest.Aggregate,
        days: Int = CompletionTierConfig.COMPLETION_HISTORY_CONSIDERED
    ): CompletionTier? {
        val cheatDays = getCheatDays(dataRequest, TimePeriod.DAY)
        val excludedDays = getExcludedDays(dataRequest, TimePeriod.DAY)
        val today = LocalDate.now()
        val endOfDayMinutes = 24 * 60

        var minutesSum = 0L
        var count = 0
        for (i in 1..days) {
            val date = today.minusDays(i.toLong())
            val formatted = date.toString()
            if (cheatDays.contains(formatted)) continue
            if (excludedDays.contains(date.dayOfWeek.value)) continue
            val hhmm = getCompletionHHMM(dataRequest, date) ?: continue
            val minutes = if (hhmm == Int.MAX_VALUE) endOfDayMinutes
            else CompletionTierConfig.hhmmToMinutes(hhmm)
            minutesSum += minutes
            count++
        }

        if (count == 0) return null
        val avgMinutes = (minutesSum / count).toInt()
        return CompletionTierConfig.tierForHHMM(CompletionTierConfig.minutesToHHMM(avgMinutes))
    }

    // endregion

    fun getProjectsTargetCompletionSummaryData(period: TimePeriod): Map<String, ProjectTargetCompletionData> {
        val result = mutableMapOf<String, ProjectTargetCompletionData>()
        for (projectName in sortedProjectList) {
            // if the project does not have a target for the given period, skip it
            if (getTarget(DataRequest.ProjectSpecific(projectName), period) == null) {
                continue
            }
            val dataRequest = DataRequest.ProjectSpecific(projectName)
            val completion = getStreakCompletion(dataRequest, period)
            val color = projectSpecificData[projectName]?.color ?: "#CCCCCC"

            result[projectName] = ProjectTargetCompletionData(
                name = projectName,
                color = color,
                completion = completion
            )
        }
        return result
    }

    // endregion
}

fun getPeriodicDateAtOffset(period: TimePeriod, offset: Int): LocalDate {
    if (offset < 0) {
        throw IllegalArgumentException("Offset must be greater than or equal to 0")
    }
    val today = LocalDate.now()
    return when (period) {
        TimePeriod.DAY -> today.minusDays(offset.toLong())
        TimePeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .minusWeeks(offset.toLong())

        TimePeriod.MONTH -> today.with(TemporalAdjusters.firstDayOfMonth())
            .minusMonths(offset.toLong())

        TimePeriod.YEAR -> today.with(TemporalAdjusters.firstDayOfYear())
            .minusYears(offset.toLong())
    }
}

fun getPeriodicDates(period: TimePeriod, reps: Int = 1): List<LocalDate> {
    return (0..reps - 1).reversed().map { i ->
        getPeriodicDateAtOffset(period, i)
    }
}
