package com.aught.wakawaka.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date // Keep Date if you want Moshi to parse the ISO strings

// region ENUMS

enum class GraphMode {
    Daily,
    Weekly
}

enum class WakaWidgetTheme {
    Dark,
    Light
}

enum class WakaURL(val url: String) {
    WAKATIME("https://api.wakatime.com/api/v1/"),
    WAKAPI("https://wakapi.dev/api/compat/wakatime/v1/")
}

enum class DayOfWeek(val index: Int, val text: String) {
    MONDAY(1, "Monday"),
    TUESDAY(2, "Tuesday"),
    WEDNESDAY(3, "Wednesday"),
    THURSDAY(4, "Thursday"),
    FRIDAY(5, "Friday"),
    SATURDAY(6, "Saturday"),
    SUNDAY(7, "Sunday");
}

// endregion


// --- Innermost classes ---

// Represents the total coding activity for a specific category, project, language, etc.
@JsonClass(generateAdapter = true)
data class ItemStat(
    val name: String,
    @Json(name = "total_seconds") val totalSeconds: Float, // Added @Json
    val percent: Float,
    val digital: String,
    val text: String,
    val hours: Int,
    val minutes: Int,
    val seconds: Int?, // Added 'seconds' as nullable Int based on log
    val decimal: String?, // <--- Added 'decimal' as nullable String based on log
    val color: String? // Added 'color' as nullable String based on log
)

// Represents the grand total coding activity for a specific day
@JsonClass(generateAdapter = true)
data class DailyGrandTotal(
    val digital: String,
    val hours: Int,
    val minutes: Int,
    @Json(name = "total_seconds") val totalSeconds: Float, // Added @Json
    val text: String,
    val decimal: String? // <--- Added 'decimal' as nullable String based on log
)

// Represents the date/time range for a specific daily summary
@JsonClass(generateAdapter = true)
data class DailyRange(
    val date: String, // Keep as String if you prefer, or change back to Date if using Date adapter
    val start: Date, // <--- Keep as Date if using Date adapter
    val end: Date,   // <--- Keep as Date if using Date adapter
    val text: String,
    val timezone: String
)

// --- Mid-level class ---

// Represents the coding activity for a single day in the "data" array
@JsonClass(generateAdapter = true)
data class DailySummaryData(
    @Json(name = "grand_total") val grandTotal: DailyGrandTotal, // Added @Json
    val projects: List<ItemStat>,
    val range: DailyRange,
    // Include other lists if they appear in your JSON and you want to parse them,
    // marking them as nullable lists if they might be absent on some days.
    val categories: List<ItemStat>? = null,
    val languages: List<ItemStat>? = null,
    val editors: List<ItemStat>? = null,
    @Json(name = "operating_systems") val operatingSystems: List<ItemStat>? = null,
    val dependencies: List<ItemStat>? = null,
    val machines: List<ItemStat>? = null,
)

// --- Top-level class ---

// Represents the entire API response for the /summaries endpoint
@JsonClass(generateAdapter = true)
data class SummariesResponse(
    val data: List<DailySummaryData>,
    @Json(name = "cumulative_total") val cumulativeTotal: CumulativeTotal? = null, // Added @Json
    @Json(name = "daily_average") val dailyAverage: DailyAverage? = null, // Added @Json
    val start: Date? = null, // <--- Keep as Date? if using Date adapter
    val end: Date? = null    // <--- Keep as Date? if using Date adapter
)

// Ensure these match the top-level cumulative/average structure from the original docs
// If your log showed these nested 'decimal' and 'total_seconds' are Strings, adjust accordingly.
// Based on the *example* response structure from the docs, cumulative/average decimal was a string.
@JsonClass(generateAdapter = true)
data class CumulativeTotal(
    val text: String,
    val digital: String,
    val decimal: String, // <--- Keep as String based on example docs
    val seconds: Float // Added @Json
)

@JsonClass(generateAdapter = true)
data class DailyAverage(
    val holidays: Int,
    @Json(name = "days_minus_holidays") val daysMinusHolidays: Int,
    @Json(name = "days_including_holidays") val daysIncludingHolidays: Int,
    val text: String,
    val seconds: Int,
)

// region DATA CLASSES FOR STREAKS

@JsonClass(generateAdapter = true)
data class StreakData(
    val count: Int,
    val updatedAt: String, // in the format YYYY-MM-DD
)

// endregion

// region DATA CLASSES FOR AGGREGATE DATA

@JsonClass(generateAdapter = true)
data class ProjectStats(
    val name: String,
    val totalSeconds: Int,
)

@JsonClass(generateAdapter = true)
data class DailyAggregateData(
    val date: String, // in the format YYYY-MM-DD
    val totalSeconds: Int,
    val projects: List<ProjectStats>
)


@JsonClass(generateAdapter = true)
data class AggregateData(
    val dailyRecords: Map<String, DailyAggregateData>,
    val dailyTargetHours: Float?,
    val weeklyTargetHours: Float?,
    val dailyStreak: StreakData?,
    val weeklyStreak: StreakData?,
    val excludedDaysFromDailyStreak: List<Int>, // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
)

// endregion

// region DATA CLASSES FOR SPECIFIC PROJECT DATA


@JsonClass(generateAdapter = true)
data class ProjectSpecificData(
    val name: String,
    val color: String,
    // amount of time in seconds spent on this project mapped to the date (yyyy-mm-dd)
    val dailyDurationInSeconds: Map<String, Int>,
    val dailyTargetHours: Float?,
    val weeklyTargetHours: Float?,
    val dailyStreak: StreakData?,
    val weeklyStreak: StreakData?,
    val excludedDaysFromDailyStreak: List<Int>, // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
)

// endregion

// region STATS DATA CLASSES

data class DurationStats(
    val today: Int,
    val last7Days: Int,
    val last30Days: Int,
    val lastYear: Int,
    val allTime: Int
)

data class WakaStatistics(
    val aggregateStats: DurationStats,
    val projectStats: Map<String, DurationStats>
)

// endregion


// region DATA CLASSES FOR NOTIFICATION DATA

@JsonClass(generateAdapter = true)
data class NotificationData(
    val lastAggregateDailyTargetNotificationDate: String, // in the format YYYY-MM-DD
    val lastAggregateWeeklyTargetNotificationDate: String, // in the format YYYY-MM-DD
    val lastProjectDailyNotificationDates: Map<String, String>, // project_name to date in the format YYYY-MM-DD
    val lastProjectWeeklyNotificationDates: Map<String, String> // project_name to date in the format YYYY-MM-DD
)

// endregion
