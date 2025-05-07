package com.aught.wakawaka.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaWidgetTheme
import kotlin.math.min
import kotlin.math.roundToInt

class WakaWidgetComponents {
    companion object {
        @Composable
        fun TargetLine(targetHours: Float, maxHours: Float, theme: WakaWidgetTheme) {
            // Target line - positioned at a specific height from bottom
            // For example, if your target is 4 hours (3600*4 seconds)
            val targetHeight = (WakaWidgetHelpers.GRAPH_HEIGHT * min(
                1f,
                (3600 * targetHours) / (3600 * maxHours)
            ) + WakaWidgetHelpers.GRAPH_BOTTOM_PADDING + WakaWidgetHelpers.DATE_TEXT_HEIGHT)

            val targetText =
                WakaHelpers.durationInSecondsToDurationString((targetHours * 3600).roundToInt())

            Box(
                modifier = GlanceModifier.fillMaxSize(),
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(bottom = (targetHeight + 5).dp, start = 15.dp),

                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = targetText,
                        style = TextStyle(
                            color = when (theme) {
                                WakaWidgetTheme.Dark -> ColorProvider(
                                    day = Color.White,
                                    night = Color.White
                                )

                                WakaWidgetTheme.Light -> ColorProvider(
                                    day = Color.Black,
                                    night = Color.Black
                                )
                            },
                            fontSize = 10.sp,
                            textAlign = TextAlign.Start
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(bottom = targetHeight.dp),

                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val numberOfDashes = 10;

                    (1..numberOfDashes).forEach {
                        // Add the dotted line
                        Box(
                            modifier = GlanceModifier
                                .width((WakaWidgetHelpers.GRAPH_WIDTH / numberOfDashes).dp)
                                .height(1.dp) // Line thickness
                                .padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = GlanceModifier.fillMaxSize().background(
                                    when (theme) {
                                        WakaWidgetTheme.Dark -> Color.Gray
                                        WakaWidgetTheme.Light -> Color.LightGray
                                    }
                                )
                            ) {}
                        }
                    }

                }
            }
        }
    }
}
