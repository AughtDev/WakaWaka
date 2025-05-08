package com.aught.wakawaka.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaWidgetTheme
import com.aught.wakawaka.utils.ColorUtils
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

                    horizontalAlignment = Alignment.Start,
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
                        modifier = GlanceModifier.wrapContentWidth().background(Color.Black).cornerRadius(3.dp).padding(2.dp)
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

        @Composable
        fun DurationScale(numMarkers: Int, maxHours: Float, textColor: Color) {
            val interval = (maxHours / numMarkers).toInt()
            val intervalDp = (WakaWidgetHelpers.GRAPH_HEIGHT * min(
                1f, interval / maxHours
            ))
            val heightOfUnitMarker = 20
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .padding(bottom = (WakaWidgetHelpers.DATE_TEXT_HEIGHT + WakaWidgetHelpers.GRAPH_BOTTOM_PADDING).dp),
                verticalAlignment = Alignment.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "h",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(day = textColor, night = textColor),
                        textAlign = TextAlign.Start
                    ),
                    modifier = GlanceModifier.padding(bottom = 2.dp).width(20.dp)
                )
                (1..numMarkers).reversed().forEach {

                    Column(
                        modifier = GlanceModifier.height(intervalDp.dp).width(25.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier.height(heightOfUnitMarker.dp).fillMaxWidth().padding(end = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier.width(5.dp).height(2.dp).cornerRadius(2.dp)
                                    .background(Color.White),
                            ) {}
                            Box(modifier = GlanceModifier.width(3.dp)) {}
                            Text(
                                text = "${it * interval}",
                                style = TextStyle(
                                    fontSize = 8.sp,
                                    color = ColorProvider(day = textColor, night = textColor),
                                )
                            )
                        }
                    }
                }
                Box(
                    modifier = GlanceModifier.height((heightOfUnitMarker/2).dp).width(25.dp),
                    contentAlignment = Alignment.BottomStart
                ){
                    Box(
                        modifier = GlanceModifier.width(15.dp).height(2.dp).cornerRadius(2.dp)
                            .background(Color.White),
                    ) {}
                }

            }
        }

        @Composable
        fun StreakDisplay(streak: Int, hitTargetToday: Boolean) {
            val trueStreak = if (hitTargetToday) streak + 1 else streak
            val streakColors = ColorUtils.getStreakColors(trueStreak)

            Text(
                text = trueStreak.toString(),
                style = TextStyle(
                    color = ColorProvider(
                        day = streakColors[0], night = streakColors[0]
                    ),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                ),
            )
        }
    }


}
