package com.aught.wakawaka.widget

import com.aught.wakawaka.data.DailyAggregateData
import com.aught.wakawaka.data.ProjectStats
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
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaWidgetTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor
import kotlin.math.min


class WakaWidget : GlanceAppWidget() {


    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MyContent()
        }
    }

    //    private val MAX_HOURS = 12;
    private val TIME_WINDOW_PROPORTION = 0.5f;
    private val GRAPH_HEIGHT = 80;
    private val GRAPH_WIDTH = 330;
    private val GRAPH_BOTTOM_PADDING = 10;
    private val DATE_TEXT_HEIGHT = 20;

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun generateDailyData(data: Map<String, DailyAggregateData>?): List<DailyAggregateData> {
        val today = LocalDate.now()

        val dailyData: MutableList<DailyAggregateData> = mutableListOf()

        (0..6).forEach { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val formattedDate = date.format(dateFormatter)
            if (data?.containsKey(formattedDate) == true) {
                dailyData.add(data[formattedDate]!!)
            } else {
                dailyData.add(DailyAggregateData(formattedDate, 0.0, emptyList()))
            }
        }

        dailyData.reverse()
        return dailyData.toList()
    }

    private fun generateWeeklyData(data: Map<String, DailyAggregateData>?): List<DailyAggregateData> {
        val today = LocalDate.now()

        val weeklyData: MutableList<DailyAggregateData> = mutableListOf()

        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        (0..6).forEach { weekOffset ->
            val weekStart = currentWeekStart.minusWeeks(weekOffset.toLong())
            val weekFormattedDate = weekStart.format(dateFormatter)
            var totalSeconds = 0.0;
            val projectsData = mutableMapOf<String, Double>()

            (0..6).forEach { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                val dayFormattedDate = date.format(dateFormatter)

                if (data?.containsKey(dayFormattedDate) == true) {
                    val dayData = data[dayFormattedDate]!!
                    totalSeconds += dayData.totalSeconds
                    dayData.projects.forEach {
                        if (projectsData.containsKey(it.name)) {
                            projectsData[it.name] = projectsData[it.name]!! + it.totalSeconds
                        } else {
                            projectsData[it.name] = it.totalSeconds
                        }
                    }
                }
            }

            val projects = projectsData.map {
                ProjectStats(it.key, it.value)
            }
            weeklyData.add(DailyAggregateData(weekFormattedDate, totalSeconds, projects))
        }

        weeklyData.reverse()
        return weeklyData.toList()
    }

    @Composable
    private fun MyContent() {
        val context = LocalContext.current
        val processedData = WakaDataWorker.loadProcessedData(context)

        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        // create a graph mode state
        var graphMode = remember { mutableStateOf(GraphMode.Daily) }

        val dailyData = generateDailyData(processedData)
        val weeklyData = generateWeeklyData(processedData)

        val dailyTargetInHours = prefs.getFloat(WakaHelpers.DAILY_TARGET_HOURS, 0f)
        val weeklyTargetInHours = prefs.getFloat(WakaHelpers.WEEKLY_TARGET_HOURS, 0f)


        val targetInHours =
            when (graphMode.value) {
                GraphMode.Daily -> dailyTargetInHours
                GraphMode.Weekly -> weeklyTargetInHours
            }

        val maxHours =
            when (graphMode.value) {
                GraphMode.Daily -> 24 * TIME_WINDOW_PROPORTION
                GraphMode.Weekly -> 24 * 7 * TIME_WINDOW_PROPORTION
            }

        val streak = WakaDataWorker.loadStreak(context, graphMode.value);

        val hitTargetToday: Boolean = when (graphMode.value) {
            GraphMode.Daily -> dailyData[dailyData.lastIndex].totalSeconds / 3600 >= dailyTargetInHours
            GraphMode.Weekly -> weeklyData[dailyData.lastIndex].totalSeconds / 3600 >= weeklyTargetInHours
        }

        val theme = when (prefs.getInt(WakaHelpers.THEME, 0)) {
            0 -> WakaWidgetTheme.Light
            1 -> WakaWidgetTheme.Dark
            else -> WakaWidgetTheme.Dark
        }

        val primaryColor = if (theme == WakaWidgetTheme.Dark) {
            ColorProvider(day = Color.White, night = Color.White)
        } else {
            ColorProvider(day = Color.Black, night = Color.Black)
        }


        Box(
            modifier = GlanceModifier.fillMaxSize().background(
                (when (theme) {
                    WakaWidgetTheme.Dark -> Color.Black
                    WakaWidgetTheme.Light -> Color.White
                }).copy(alpha = 0.2f)
            )
        )
        {
            if (hitTargetToday) {
                Box(
                    modifier = GlanceModifier.width(5.dp).height(5.dp).cornerRadius(5.dp)
                        .background(primaryColor),
                ) {}
            }
            Text(
                text = (streak + (if (hitTargetToday) 1 else 0)).toString(),
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                ),
                modifier = GlanceModifier.height(100.dp).padding(horizontal=10.dp)
            )
            Column(
                modifier = GlanceModifier.fillMaxSize(),
//                .background(Color.Black),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // top bar
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
                // graph container
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier
                            .background(Color.Transparent)
                            .fillMaxWidth().fillMaxHeight()
                            .padding(bottom = GRAPH_BOTTOM_PADDING.dp),
                    ) {
                        // map all days or weeks depending on graph mode
                        when (graphMode.value) {
                            GraphMode.Daily -> dailyData
                            GraphMode.Weekly -> weeklyData
                        }.forEach {
                            Column(
                                modifier = GlanceModifier
//                            .background(Color.Green)
                                    .width(47.dp).padding(horizontal = 3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                var barColor = ColorProvider(day = Color.Gray, night = Color.Gray)
                                if (it.totalSeconds / 3600 >= targetInHours) {
                                    barColor = primaryColor
                                }

                                Column(
                                    // 100% height
                                    modifier = GlanceModifier
                                        .wrapContentHeight()
                                        .cornerRadius(3.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    it.projects
                                        .sortedBy { it.totalSeconds }
                                        .forEach {
                                            Box(
                                                modifier = GlanceModifier.fillMaxWidth()
                                                    .height(
                                                        (GRAPH_HEIGHT * min(
                                                            1.0,
                                                            it.totalSeconds / (3600 * maxHours)
                                                        )).dp
                                                    )
//                                                .background(WakaHelpers.projectNameToColor(it.name))
                                                    .background(barColor)
                                                    .padding(4.dp)
                                            ) {}
                                        }
                                }
                                // get the day,month and year from date of format yyyy-mm-dd
                                val date = it.date.split("-")
                                Box(
                                    modifier = GlanceModifier.height(DATE_TEXT_HEIGHT.dp)
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
                    TargetLine(targetInHours, maxHours, theme)
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

    @Composable
    private fun TargetLine(targetHours: Float, maxHours: Float, theme: WakaWidgetTheme) {
        // Target line - positioned at a specific height from bottom
        // For example, if your target is 4 hours (3600*4 seconds)
        val targetHeight = (GRAPH_HEIGHT * min(
            1f,
            (3600 * targetHours) / (3600 * maxHours)
        ) + GRAPH_BOTTOM_PADDING + DATE_TEXT_HEIGHT)

        val totalMinutes = targetHours * 60
        val numHours = floor(totalMinutes / 60).toInt()
        val numMinutes = (totalMinutes % 60).toInt()

        var targetText = ""
        if (numHours > 0) {
            targetText += "$numHours h"
        }
        if (numMinutes > 0) {
            if (targetText.isNotEmpty()) {
                targetText += " "
            }
            targetText += "$numMinutes m"
        }

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
                            .width((GRAPH_WIDTH / numberOfDashes).dp)
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

                // Optional: Add a label for the target
//            Text(
//                text = "Target: 4h",
//                style = TextStyle(
//                    color = ColorProvider(day = Color.Yellow, night = Color.Yellow),
//                ),
//                modifier = GlanceModifier.padding(top = targetHeight + 5.dp)
//            )
            }
        }
    }


}
