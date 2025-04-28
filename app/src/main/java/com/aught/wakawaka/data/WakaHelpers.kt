package com.aught.wakawaka.data

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import android.graphics.Color as AndroidColor

class WakaHelpers {
    companion object {
        val PREFS = "wakawaka_prefs"
        val TARGET_HOURS = "target_hours"
        /**
         * Deterministically generates a distinct, bright color for a given project name.
         * The color is generated based on the project name's hash code, mapped to the HSV color space.
         *
         * @param projectName The name of the project.
         * @return A Compose UI Color. Returns a default color (Gray) for null or empty names.
         */
        fun projectNameToColor(projectName: String?): Color {
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
            val saturation = 0.7f // Adjust between 0.5f and 1.0f for desired intensity
            val value = 0.9f      // Adjust between 0.7f and 1.0f for desired brightness

            // Convert HSV values to an Android ARGB color integer
            val hsv = floatArrayOf(hue.toFloat(), saturation, value)
            val argb = AndroidColor.HSVToColor(hsv)

            // Convert the Android ARGB color integer to a Compose Color
            return Color(argb)
        }
    }
}
