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

// endregion


// --- Innermost classes ---

// Represents the total coding activity for a specific category, project, language, etc.
@JsonClass(generateAdapter = true)
data class ItemStat(
    val name: String,
    @Json(name = "total_seconds") val totalSeconds: Double, // Added @Json
    val percent: Double,
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
    @Json(name = "total_seconds") val totalSeconds: Double, // Added @Json
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
    val seconds: Double // Added @Json
)

@JsonClass(generateAdapter = true)
data class DailyAverage(
    val holidays: Int,
    @Json(name = "days_minus_holidays") val daysMinusHolidays: Int,
    @Json(name = "days_including_holidays") val daysIncludingHolidays: Int,
    val text: String,
    val seconds: Int,
)

// region DATA CLASSES FOR WIDGET

@JsonClass(generateAdapter = true)
data class WakaProjectData(
    val name: String,
    val totalSeconds: Double,
)

@JsonClass(generateAdapter = true)
data class WakaData(
    val date: String, // in the format YYYY-MM-DD
    val totalSeconds: Double,
    val projects: List<WakaProjectData>
)

// endregion

// region DATA CLASSES FOR STREAKS

@JsonClass(generateAdapter = true)
data class WakaStreak(
    val count: Int,
    val updatedAt: String, // in the format YYYY-MM-DD
)

// endregion

