package com.aught.wakawaka.widget.project

import android.content.Context
import androidx.glance.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.layout.Box
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.aught.wakawaka.data.DailyAggregateData
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.ProjectStats
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaWidgetTheme
import com.aught.wakawaka.utils.ColorUtils
import com.aught.wakawaka.widget.WakaWidgetComponents
import com.aught.wakawaka.widget.WakaWidgetHelpers
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

class WakaProjectWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MyContent()
        }
    }


    private fun generateDailyData(data: Map<String, Int>?): List<Int> {
        val today = LocalDate.now()
        val dailyData: MutableList<Int> = mutableListOf()


        (0..6).forEach { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val formattedDate = WakaHelpers.dateToYYYYMMDD(date)
            if (data?.containsKey(formattedDate) == true) {
                dailyData.add(data[formattedDate]!!)
            } else {
                dailyData.add(0)
            }
        }

        dailyData.reverse()
        return dailyData.toList()
    }

    private fun generateWeeklyData(data: Map<String, Int>?): List<Int> {
        val today = LocalDate.now()

        val weeklyData: MutableList<Int> = mutableListOf()

        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        (0..6).forEach { weekOffset ->
            val weekStart = currentWeekStart.minusWeeks(weekOffset.toLong())
            var totalSeconds = 0;

            (0..6).forEach { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                val dayFormattedDate = WakaHelpers.dateToYYYYMMDD(date)

                if (data?.containsKey(dayFormattedDate) == true) {
                    totalSeconds += data[dayFormattedDate]!!
                }
            }
            weeklyData.add(totalSeconds)
        }

        weeklyData.reverse()
        return weeklyData.toList()
    }

    private fun getProjectData(context: Context): ProjectSpecificData? {
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        val projectData = WakaDataWorker.loadProjectSpecificData(context)
        val widgetProject = prefs.getString(WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET, null)

        if (widgetProject != null) {
            val project = projectData[widgetProject]
            if (project != null) {
                return project
            }
        }
        return null
    }

    @Composable
    private fun MyContent() {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        val projectData = getProjectData(context)


        if (projectData == null) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No project assigned",
                )
            }
        } else {

            // create a graph mode state
            var graphMode = remember { mutableStateOf(GraphMode.Daily) }

            val dailyData = generateDailyData(projectData.dailyDurationInSeconds)
            val weeklyData = generateWeeklyData(projectData.dailyDurationInSeconds)

            val dailyTargetInHours = projectData.dailyTargetHours
            val weeklyTargetInHours = projectData.weeklyTargetHours


            val targetInHours =
                when (graphMode.value) {
                    GraphMode.Daily -> dailyTargetInHours
                    GraphMode.Weekly -> weeklyTargetInHours
                }

            val maxHours =
                when (graphMode.value) {
                    GraphMode.Daily -> 24 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
                    GraphMode.Weekly -> 24 * 7 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
                }

            val streak = if (projectData != null) {
                when (graphMode.value) {
                    GraphMode.Daily -> projectData.dailyStreak?.count ?: 0
                    GraphMode.Weekly -> projectData.weeklyStreak?.count ?: 0
                }
            } else 0

            val hitTargetToday: Boolean = when (graphMode.value) {
                GraphMode.Daily -> WakaDataWorker.dailyTargetHit(
                    projectData?.dailyDurationInSeconds?.mapValues { it.value } ?: emptyMap(),
                    dailyTargetInHours)

                GraphMode.Weekly -> WakaDataWorker.weeklyTargetHit(
                    projectData?.dailyDurationInSeconds?.mapValues { it.value } ?: emptyMap(),
                    weeklyTargetInHours
                )
            }

            val excludedDaysSet = when (graphMode.value) {
                GraphMode.Daily -> projectData?.excludedDaysFromDailyStreak?.toSet() ?: emptySet<Int>()
                GraphMode.Weekly -> emptySet<Int>()
            }.toSet()

            val theme = when (prefs.getInt(WakaHelpers.THEME, 0)) {
                0 -> WakaWidgetTheme.Light
                1 -> WakaWidgetTheme.Dark
                else -> WakaWidgetTheme.Dark
            }

            val projectColor =
                runCatching { Color(projectData.color.toColorInt()) }.getOrNull() ?: WakaHelpers.projectNameToColor(projectData.name)

            val primaryColor = if (theme == WakaWidgetTheme.Dark) {
                ColorProvider(day = projectColor, night = projectColor)
            } else {
                ColorProvider(day = projectColor, night = projectColor)
            }

            val textColor = ColorUtils.desaturate(projectColor, 0.5f)

            println("primary color is $primaryColor")
            Box(
                modifier = GlanceModifier.fillMaxSize().background(
                    (when (theme) {
                        WakaWidgetTheme.Dark -> Color.Black
                        WakaWidgetTheme.Light -> Color.White
                    }).copy(alpha = 0.2f)
                )
            )
            {
                WakaWidgetComponents.DurationScale(5, maxHours, textColor)
                Box(
                    modifier = GlanceModifier.height(100.dp).padding(start = 10.dp, top = 20.dp)
                ) {
                    WakaWidgetComponents.StreakDisplay(streak, hitTargetToday)
                }
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
                        Text(
                            text = projectData.name.uppercase(),
                            style = TextStyle(
                                color = primaryColor,
                                fontSize = 16.sp,
                            ),
                        )
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
                                .padding(bottom = WakaWidgetHelpers.GRAPH_BOTTOM_PADDING.dp),
                        ) {
                            // map all days or weeks depending on graph mode
                            when (graphMode.value) {
                                GraphMode.Daily -> dailyData
                                GraphMode.Weekly -> weeklyData
                            }.forEachIndexed { i, it ->
                                val idx = (if (graphMode.value == GraphMode.Daily) dailyData else weeklyData).size - i - 1
                                // if graph is weekly, move to the first day of the week
                                val date = if (graphMode.value == GraphMode.Weekly) {
                                    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                        .minusWeeks(idx.toLong())
                                } else {
                                    LocalDate.now().minusDays(idx.toLong())
                                }

                                Column(
                                    modifier = GlanceModifier
//                            .background(Color.Green)
                                        .width(47.dp).padding(horizontal = 3.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val barColor =
                                        if (
                                            targetInHours == null ||
                                            // if the day is in the exclusion list, use the primary color
                                            date.dayOfWeek.value in excludedDaysSet ||
                                            it > (targetInHours * 3600)
                                        ) primaryColor
                                        else
                                            ColorProvider(day = Color.Gray, night = Color.Gray)

                                    Column(
                                        // 100% height
                                        modifier = GlanceModifier
                                            .wrapContentHeight()
                                            .cornerRadius(3.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Box(
                                            modifier = GlanceModifier.fillMaxWidth()
                                                .height(
                                                    (WakaWidgetHelpers.GRAPH_HEIGHT * min(
                                                        1f,
                                                        it / (3600 * maxHours)
                                                    )).dp
                                                )
//                                                .background(WakaHelpers.projectNameToColor(it.name))
                                                .background(barColor)
                                                .padding(4.dp)
                                        ) {}
                                    }
                                    // get the day,month and year from date of format yyyy-mm-dd
                                    val date = WakaHelpers.dateToYYYYMMDD(date).split("-")
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
                                                color = ColorProvider(day = textColor, night = textColor)
                                            )
                                        )
                                    }

                                }
                            }
                        }

                        // Add this Box for the target line overlay
                        if (targetInHours != null) WakaWidgetComponents.TargetLine(targetInHours, maxHours, theme)
                    }
                }
            }
        }
    }
}

