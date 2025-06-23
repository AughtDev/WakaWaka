package com.aught.wakawaka.data

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.floor
import android.graphics.Color as AndroidColor

class WakaHelpers {
    companion object {
        val PREFS = "wakawaka_prefs"

        val AGGREGATE_DATA = "waka_aggregate_data"
        val INITIAL_AGGREGATE_DATA =
            AggregateData(emptyMap(), null, null, null, null, listOf(DayOfWeek.SUNDAY.index))

        val PROJECT_SPECIFIC_DATA = "waka_project_data"

        val WAKA_STATISTICS = "waka_statistics"

        val WAKATIME_API = "wakatime_api"
        val WAKAPI_API = "wakapi_api"
        val WAKA_URL = "https://api.wakatime.com/api/v1/"

        val THEME = "theme"

        val ZERO_DAY = "2000-10-02"

        val ALL_PROJECTS_ID = "All"

        val NOTIFICATION_DATA = "last_notification_date"

        val PROJECT_ASSIGNED_TO_PROJECT_WIDGET = "project_assigned_to_project_widget"

        val MAX_LABEL_SIZE = 20

        val PROJECT_COLOR_LUMINANCE = 0.9f

        val LAST_FETCH_TIMESTAMP = "last_fetch_timestamp"

        val MIN_FETCH_INTERVAL = 1 * 60 * 1000 // update at most every 1 minute

        //region INITIAL DATA

        val INITIAL_WAKA_STATISTICS = WakaStatistics(
            DurationStats(0, 0, 0, 0, 0),
            emptyMap()
        )

        val INITIAL_NOTIFICATION_DATA = NotificationData(
            WakaHelpers.ZERO_DAY, WakaHelpers.ZERO_DAY, emptyMap(), emptyMap()
        )

        //endregion



        fun getYYYYMMDDDateFormatter(): DateTimeFormatter {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }

        /**
         * Deterministically generates a distinct, bright color for a given project name.
         * The color is generated based on the project name's hash code, mapped to the HSV color space.
         *
         * @param projectName The name of the project.
         * @return A Compose UI Color. Returns a default color (Gray) for null or empty names.
         */
        fun projectNameToColor(projectName: String?): Color {
            println("projectNameToColor: $projectName")
            // Use a default color for null or empty project names
            if (projectName.isNullOrEmpty()) {
                return Color.Gray
            }

            // Get the hash code of the project name
            val hashCode = projectName.hashCode()

            // Map the hash code to a hue value (0 to 359)
            // Using abs() ensures positive values, modulo 360 wraps it around the color wheel
            val hue = abs(hashCode) % 360

            // Define saturation and value for bright and vivid colors.
            // Values are typically between 0.0f and 1.0f.
            // High saturation and value contribute to the 'bright' and 'warm' feel
            // by making colors vibrant, even if the hue itself is technically 'cool'.
            // If you strictly only want warm hues (red, orange, yellow), you'd need to
            // map the hash code to a specific range within 0-360 (e.g., 0-120 and 330-360),
            // but this would limit the number of distinct hues significantly.
            // Using the full spectrum with high S and V usually provides the best visual distinctness.
            val saturation = 0.6f // Adjust between 0.5f and 1.0f for desired intensity
            val value = PROJECT_COLOR_LUMINANCE      // Adjust between 0.7f and 1.0f for desired brightness

            // Convert HSV values to an Android ARGB color integer
            val hsv = floatArrayOf(hue.toFloat(), saturation, value)
            val argb = AndroidColor.HSVToColor(hsv)

            // Convert the Android ARGB color integer to a Compose Color
            return Color(argb)
        }

        fun base64Encode(token: String): String {
            val bytes = token.toByteArray()
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }

        fun durationInSecondsToDurationString(durationInSeconds: Int, hoursSuffix: String = "h", minutesSuffix: String = "m"): String {
            // the expected return format is 5h 34m
            val totalMinutes = durationInSeconds / 60
            val numHours = floor(totalMinutes.toFloat() / 60).toInt()
            val numMinutes = (totalMinutes % 60).toInt()

            if (totalMinutes == 0) {
                return "0m"
            }

            var durationString = ""
            if (numHours > 0) {
                durationString += "$numHours$hoursSuffix"
            }
            if (numMinutes > 0) {
                if (durationString.isNotEmpty()) {
                    durationString += " "
                }
                durationString += "$numMinutes$minutesSuffix"
            }

            return durationString
        }

        fun yyyyMMDDToDate(date: String): LocalDate {
            return LocalDate.parse(date, getYYYYMMDDDateFormatter())
        }

        fun dateToYYYYMMDD(date: LocalDate): String {
            return date.format(getYYYYMMDDDateFormatter())
        }

        fun truncateLabel(label: String, maxLen: Int = MAX_LABEL_SIZE): String {
            return if (label.length > maxLen) {
                label.substring(0, maxLen - 3) + "..."
            } else {
                label
            }
        }

        fun getDateSuffix(date: Int): String {
            return when (date) {
                1, 21, 31 -> "st"
                2, 22 -> "nd"
                3, 23 -> "rd"
                else -> "th"
            }
        }
    }
}
