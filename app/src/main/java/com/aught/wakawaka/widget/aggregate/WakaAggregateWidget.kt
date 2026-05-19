package com.aught.wakawaka.widget.aggregate

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.Image
import androidx.glance.ImageProvider
import com.aught.wakawaka.MainActivity
import com.aught.wakawaka.data.CompletionTier
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.ProjectTargetCompletionData
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaWidgetTheme
import com.aught.wakawaka.data.getPeriodicDates
import com.aught.wakawaka.ui.theme.Grotesk
import com.aught.wakawaka.widget.WakaWidgetComponents
import com.aught.wakawaka.widget.WakaWidgetHelpers
import java.time.LocalDate
import kotlin.math.min

class WakaAggregateWidget : GlanceAppWidget() {
    companion object {
        const val NUM_BARS = 7
    }


    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MyContent()
        }
    }

    @Composable
    private fun MyContent() {
        val context = LocalContext.current
        val wakaDataHandler = WakaDataHandler.fromContext(context)
        val dataRequest = DataRequest.Aggregate

//        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        // create a graph mode state
        var graphMode = remember { mutableStateOf(GraphMode.Daily) }

        val timePeriod = when (graphMode.value) {
            GraphMode.Daily -> TimePeriod.DAY
            GraphMode.Weekly -> TimePeriod.WEEK
        }
        val data = wakaDataHandler.getPeriodicDurationsInSeconds(dataRequest, timePeriod, NUM_BARS)
        val dates = getPeriodicDates(timePeriod, NUM_BARS)

        // Get cheat days for this data request
        val cheatDays = wakaDataHandler.getCheatDays(dataRequest, timePeriod)
        val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()

        val targetInHours = wakaDataHandler.getTarget(dataRequest, timePeriod)

//        val maxHours =
//            when (graphMode.value) {
//                GraphMode.Daily -> 24 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
//                GraphMode.Weekly -> 24 * 7 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
//            }
        val maxHours =
            (if (targetInHours != null) {
                targetInHours * 2
            } else {
                // maximum duration at 3/4 of the day
                val maxDurationInHours = data.maxOf { it / 3600f }
                maxDurationInHours * 1.25f

            }).coerceIn(
                4f, if (graphMode.value == GraphMode.Daily) {
                    24 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
                } else {
                    24 * 7 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
                }
            )

        val streak = wakaDataHandler.getStreak(dataRequest, timePeriod).count

        // Check if today is a cheat day
        val today = LocalDate.now()
        val todayFormatted = today.format(dateFormatter)
        val todayWeekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(dateFormatter)
        val isTodayCheatDay = when (timePeriod) {
            TimePeriod.DAY -> cheatDays.contains(todayFormatted)
            TimePeriod.WEEK -> cheatDays.contains(todayWeekStart)
            else -> false
        }

        val hitTargetToday = wakaDataHandler.targetHit(dataRequest, timePeriod) || isTodayCheatDay

        val excludedDays = wakaDataHandler.getExcludedDays(dataRequest, timePeriod)

//        val theme = when (prefs.getInt(WakaHelpers.THEME, 0)) {
//            0 -> WakaWidgetTheme.Light
//            1 -> WakaWidgetTheme.Dark
//            else -> WakaWidgetTheme.Dark
//        }
        val theme = WakaWidgetTheme.Dark

        val primaryColor = if (theme == WakaWidgetTheme.Dark) {
            ColorProvider(day = Color.White, night = Color.White)
        } else {
            ColorProvider(day = Color.Black, night = Color.Black)
        }

        val projectTargetSummaries =
            wakaDataHandler.getProjectsTargetCompletionSummaryData(timePeriod)

        val averageTier = if (graphMode.value == GraphMode.Daily) {
            wakaDataHandler.getAverageCompletionTier()
        } else null

        val density = context.resources.displayMetrics.density

        Box(
            modifier = GlanceModifier.fillMaxSize().background(
                (when (theme) {
                    WakaWidgetTheme.Dark -> Color.Black
                    WakaWidgetTheme.Light -> Color.White
                }).copy(alpha = 0.3f)
            )
        )
        {
            if (hitTargetToday) {
                Box(
                    modifier = GlanceModifier.width(5.dp).height(5.dp).cornerRadius(5.dp)
                        .background(primaryColor),
                ) {}
            }
            WakaWidgetComponents.DurationScale(5, maxHours, Color.White)
            Box(
                modifier = GlanceModifier
//                    .height(110.dp)
                    .padding(start = 8.dp, top = 8.dp)
            ) {
                WakaWidgetComponents.StreakDisplay(streak, hitTargetToday, projectTargetSummaries)
            }
            Column(
                modifier = GlanceModifier.fillMaxSize(),
//                .background(Color.Black),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // top bar
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(modifier = GlanceModifier.defaultWeight()) {}
                        Row {
                            Text(
                                text = "DAILY",
                                style = TextStyle(
                                    color = when (graphMode.value) {
                                        GraphMode.Daily -> primaryColor

                                        GraphMode.Weekly -> ColorProvider(
                                            day = Color.Gray,
                                            night = Color.Gray
                                        )
                                    },
                                    fontSize = 16.sp,
                                    fontWeight =
                                        when (graphMode.value) {
                                            GraphMode.Daily -> FontWeight.Bold
                                            GraphMode.Weekly -> FontWeight.Normal
                                        }

                                ),
                                modifier = GlanceModifier
                                    .clickable {
                                        graphMode.value = GraphMode.Daily
                                    }
                            )
                            Box(modifier = GlanceModifier.width(10.dp)) {}
                            Text(
                                text = "WEEKLY",
                                style = TextStyle(
                                    color = when (graphMode.value) {
                                        GraphMode.Daily -> ColorProvider(
                                            day = Color.Gray,
                                            night = Color.Gray
                                        )

                                        GraphMode.Weekly -> primaryColor
                                    },
                                    fontSize = 16.sp,
                                    fontWeight =
                                        when (graphMode.value) {
                                            GraphMode.Daily -> FontWeight.Normal
                                            GraphMode.Weekly -> FontWeight.Bold
                                        }
                                ),
                                modifier = GlanceModifier
                                    .clickable {
                                        graphMode.value = GraphMode.Weekly
                                    }
                            )

                        }
                    }


                    if (averageTier != null && averageTier != CompletionTier.None) {
                        val crownHeightDp = 20
                        val crownWidthDp = 28
                        val crownBitmap = renderTierCrown(
                            averageTier,
                            (crownWidthDp * density).toInt(),
                            (crownHeightDp * density).toInt()
                        )
                        if (crownBitmap != null) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth()
                                    .padding(horizontal = 15.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(modifier = GlanceModifier.defaultWeight()) {}
                                Image(
                                    provider = ImageProvider(crownBitmap),
                                    contentDescription = "Average completion tier",
                                    modifier = GlanceModifier
                                        .width(crownWidthDp.dp)
                                        .height(crownHeightDp.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = GlanceModifier.fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(modifier = GlanceModifier.defaultWeight()) {}
                        projectTargetSummaries.values.map {
                            Box(
                                GlanceModifier.padding(horizontal = 2.dp)
                            ) {
                                WakaWidgetComponents.ProjectTargetCompletionDisplay(it, 25, 15, 6)
                            }
                        }
                    }
                }
                // graph container
                Box(
                    // go to the app MainActivity
                    modifier = GlanceModifier.fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier
                            .background(Color.Transparent)
                            .fillMaxWidth().fillMaxHeight()
                            .padding(bottom = WakaWidgetHelpers.GRAPH_BOTTOM_PADDING.dp),
                    ) {
                        // map all days or weeks depending on graph mode
                        dates.zip(data).forEach { it ->
                            val date = it.first
                            val duration = it.second

                            // Check if this date is a cheat day
                            val dateFormatted = date.format(dateFormatter)
                            val weekStartFormatted = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(dateFormatter)
                            val isCheatDay = when (timePeriod) {
                                TimePeriod.DAY -> cheatDays.contains(dateFormatted)
                                TimePeriod.WEEK -> cheatDays.contains(weekStartFormatted)
                                else -> false
                            }

                            val barWidthDp = (WakaWidgetHelpers.GRAPH_WIDTH / NUM_BARS) - 6
                            Column(
                                modifier = GlanceModifier
//                            .background(Color.Green)
                                    .width((WakaWidgetHelpers.GRAPH_WIDTH / NUM_BARS).dp)
                                    .padding(horizontal = 3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val targetMissed =
                                    !(isCheatDay ||
                                        targetInHours == null ||
                                        date.dayOfWeek.value in excludedDays ||
                                        duration >= (targetInHours * 3600))

                                // Only compute the tier in DAY mode; weekly bars keep gray/white.
                                val tier = if (graphMode.value == GraphMode.Daily && !targetMissed) {
                                    wakaDataHandler.getCompletionTier(DataRequest.Aggregate, date)
                                } else null

                                val barColor =
                                    if (targetMissed) ColorProvider(day = Color.Gray, night = Color.Gray)
                                    else primaryColor

                                val useTierBitmap =
                                    tier != null && tier != CompletionTier.None

                                val barHeight = WakaWidgetHelpers.GRAPH_HEIGHT * min(
                                    1f,
                                    duration / (3600 * maxHours)
                                )
                                val cheatCircleSize = 16f
                                val canFitCircleInBar = barHeight >= cheatCircleSize + 4

                                Column(
                                    // 100% height
                                    modifier = GlanceModifier
                                        .wrapContentHeight()
                                        .cornerRadius(3.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // If cheat day and circle doesn't fit in bar, show it above
                                    if (isCheatDay && !canFitCircleInBar) {
                                        Box(
                                            modifier = GlanceModifier
                                                .width(cheatCircleSize.dp)
                                                .height(cheatCircleSize.dp)
                                                .cornerRadius((cheatCircleSize / 2).dp)
                                                .background(primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = GlanceModifier
                                                    .width((cheatCircleSize - 4).dp)
                                                    .height((cheatCircleSize - 4).dp)
                                                    .cornerRadius(((cheatCircleSize - 4) / 2).dp)
                                                    .background(Color.Black)
                                            ) {}
                                        }
                                    }
                                    Box(
                                        modifier = GlanceModifier.fillMaxWidth()
                                            .height(barHeight.dp)
                                            .let { if (useTierBitmap) it else it.background(barColor) }
                                            .cornerRadius(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (useTierBitmap) {
                                            val barBitmap = renderTierBar(
                                                tier!!,
                                                (barWidthDp * density).toInt(),
                                                (barHeight * density).toInt(),
                                                (3 * density).toInt()
                                            )
                                            Image(
                                                provider = ImageProvider(barBitmap),
                                                contentDescription = null,
                                                modifier = GlanceModifier.fillMaxSize()
                                            )
                                        }
                                        // If cheat day and circle fits in bar, show it centered
                                        if (isCheatDay && canFitCircleInBar) {
                                            Box(
                                                modifier = GlanceModifier
                                                    .width(cheatCircleSize.dp)
                                                    .height(cheatCircleSize.dp)
                                                    .cornerRadius((cheatCircleSize / 2).dp)
                                                    .background(Color.Black)
                                            ) {}
                                        }
                                    }
                                }
                                // get the day,month and year from date of format yyyy-mm-dd
                                val date = date.toString().split("-")
                                Box(
                                    modifier = GlanceModifier.height(WakaWidgetHelpers.DATE_TEXT_HEIGHT.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date[2] + "/" + date[1],
                                        style = TextStyle(
                                            textAlign = TextAlign.Center,
                                            fontSize = 10.sp,
                                            color = primaryColor
                                        )
                                    )
                                }

                            }
                        }
                    }

                    // Add this Box for the target line overlay
                    if (targetInHours != null) WakaWidgetComponents.TargetLine(
                        targetInHours,
                        maxHours,
                        theme
                    )
                }

//            Row(horizontalAlignment = Alignment.CenterHorizontally) {
//                Button(
//                    text = "Homes",
//                    onClick = actionStartActivity<MainActivity>()
//                )
//                Button(
//                    text = "Works",
//                    onClick = actionStartActivity<MainActivity>()
//                )
//            }
            }
        }
    }


}
