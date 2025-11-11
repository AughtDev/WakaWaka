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
import com.aught.wakawaka.data.ProjectTargetCompletionData
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaWidgetTheme
import com.aught.wakawaka.utils.ColorUtils
import com.aught.wakawaka.utils.StreakColors
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt

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
                        modifier = GlanceModifier.wrapContentWidth().background(Color.Black)
                            .cornerRadius(3.dp).padding(2.dp)
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
            val interval = (maxHours / numMarkers).toInt().coerceAtLeast(1)
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
//                Text(
//                    text = "h",
//                    style = TextStyle(
//                        fontSize = 12.sp,
//                        color = ColorProvider(day = textColor, night = textColor),
//                        textAlign = TextAlign.Start
//                    ),
//                    modifier = GlanceModifier.padding(bottom = 2.dp).width(20.dp)
//                )
                (1..numMarkers).reversed().forEach {

                    Column(
                        modifier = GlanceModifier.height(intervalDp.dp).width(25.dp)
                    ) {
                        // if beyond graph height, skip
                        if (intervalDp * it > WakaWidgetHelpers.GRAPH_HEIGHT) {
                            return@Column
                        }
                        Row(
                            modifier = GlanceModifier.height(heightOfUnitMarker.dp).fillMaxWidth()
                                .padding(end = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier.width(5.dp).height(2.dp)
                                    .cornerRadius(2.dp)
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
                    modifier = GlanceModifier.height((heightOfUnitMarker / 2).dp).width(20.dp)
                        .padding(start = 0.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(
                        modifier = GlanceModifier.width(8.dp).height(2.dp).cornerRadius(2.dp)
                            .background(Color.White),
                    ) {}
                }

            }
        }

        @Composable
        fun StreakDisplay(streak: Int, hitTargetToday: Boolean,targetCompletionData: Map<String, ProjectTargetCompletionData>?) {
            val trueStreak = if (hitTargetToday) streak + 1 else streak
            val streakColors = ColorUtils.getStreakColors(trueStreak)

//            val segments = mutableListOf<String>()

            // write out each character separately, first split them into segments
            val chars = trueStreak.toString().toCharArray()

            // get the color for each char, then create a list of pairs of char and color
            // the characters get assigned to streak colors in order, when we run out of colors, we use the last color for the rest of the characters
            val segments = mutableListOf<Pair<String, Triple<Color, Color, Color>>>()

            for (i in chars.indices) {
                val char = chars[i].toString()
                val colIdx = min(i, streakColors.colors.size - 1)
                val colors = Triple(
                    // active text color
                    streakColors.colors[colIdx],
                    // inactive text color
                    streakColors.inactiveColors[colIdx],
                    // background color
                    streakColors.bgColors[colIdx]
                )
                segments.add(Pair(char, colors))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier
//                    .background(Color.Black.copy(0.5f))
                        .cornerRadius(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.Start
                    ) {
//                    Box(
//                        modifier = GlanceModifier.background(streakColors[0]).size(8.dp).cornerRadius(4.dp).padding(horizontal = 4.dp)
//                    ) {  }
                        segments.forEachIndexed { i, (seg, colors) ->
                            var (color, inactiveColor, bgColor) = colors
                            if (!hitTargetToday) {
                                color = color.copy(0.4f)
                            }
                            val lp = if (i == 0) 8.dp else 0.dp
                            val rp = if (i == segments.size - 1) 8.dp else 0.dp
                            // background is black if only one color, else the next color in the streakColors list, the first color if last character
                            Text(
                                text = seg,
                                style = TextStyle(
                                    color = ColorProvider(
                                        day = color, night = color
                                    ),
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                ),
                                modifier = GlanceModifier.height(60.dp).padding(
                                    top = (-10).dp, start = lp, end = rp,
                                ).background(bgColor)
                            )
                        }
                    }
                }
                if (targetCompletionData != null && targetCompletionData.values.isNotEmpty()) {
                    // get number of completed projects, display as a fraction next to the streak like 4/6. 4 on top of -- on top of 6
                    // if not all projects completed, slightly transparent. use the same color as the streak text. if 2 or more colors,
                    // one color for the numerator, next color for the denominator

                    val totalProjects = targetCompletionData.size
                    val completedProjects = targetCompletionData.values.count { it.completion >= 1f }
                    val allCompleted = completedProjects == totalProjects

                    // Get colors from streak colors
                    val numeratorColor = if (streakColors.colors.size >= 2) {
                        streakColors.colors[0]
                    } else {
                        streakColors.colors[0]
                    }

                    val denominatorColor = if (streakColors.colors.size >= 2) {
                        streakColors.colors[1]
                    } else {
                        streakColors.colors[0]
                    }

                    val alpha = if (allCompleted) 1f else 0.5f

                    Box(modifier = GlanceModifier.width(8.dp)) {}

                    Column(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.padding(horizontal = 4.dp)
                    ) {
                        // Numerator (completed)
                        Text(
                            text = completedProjects.toString(),
                            style = TextStyle(
                                color = ColorProvider(
                                    day = numeratorColor.copy(alpha = alpha),
                                    night = numeratorColor.copy(alpha = alpha)
                                ),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                        // Divider line
                        Box(
                            modifier = GlanceModifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(numeratorColor.copy(alpha = alpha * 0.7f))
                        ) {}
                        // Denominator (total)
                        Text(
                            text = totalProjects.toString(),
                            style = TextStyle(
                                color = ColorProvider(
                                    day = denominatorColor.copy(alpha = alpha),
                                    night = denominatorColor.copy(alpha = alpha)
                                ),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }


        @Composable
        fun ProjectTargetCompletionDisplay(
            projectTargetCompletionData: ProjectTargetCompletionData,
            width: Int,
            height: Int,
            radius: Int
        ) {
            // a rounded rectangle colored with the project color that is transparent as the background
            // but has a bar showing the completion percentage that is not transparent from left to right

            val projectColor = runCatching {
                Color(projectTargetCompletionData.color.toColorInt())
            }.getOrNull() ?: Color.White

            val completionWidth =
                (width * projectTargetCompletionData.completion.coerceIn(0f, 1f)).toInt()

            Box(
                modifier = GlanceModifier
                    .width(width.dp)
                    .height(height.dp)
                    .cornerRadius(radius.dp)
                    .background(projectColor.copy(alpha = 0.3f))
            ) {
                // Completion bar
                Box(
                    modifier = GlanceModifier
                        .width(completionWidth.dp)
                        .height(height.dp)
                        .cornerRadius(radius.dp)
                        .background(projectColor)
                ) {}
            }
        }
    }


}
