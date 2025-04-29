package com.aught.wakawaka.widget

import com.aught.wakawaka.data.WakaData
import com.aught.wakawaka.data.WakaProjectData
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
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaStreak
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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

    private fun generateDailyData(data: Map<String, WakaData>?): List<WakaData> {
        val today = LocalDate.now()

        val dailyData: MutableList<WakaData> = mutableListOf()

        (0..6).forEach { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val formattedDate = date.format(dateFormatter)
            if (data?.containsKey(formattedDate) == true) {
                dailyData.add(data[formattedDate]!!)
            } else {
                dailyData.add(WakaData(formattedDate, 0.0, emptyList()))
            }
        }

        dailyData.reverse()
        return dailyData.toList()
    }

    private fun generateWeeklyData(data: Map<String, WakaData>?): List<WakaData> {
        val today = LocalDate.now()

        val weeklyData: MutableList<WakaData> = mutableListOf()

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
                WakaProjectData(it.key, it.value)
            }
            weeklyData.add(WakaData(weekFormattedDate, totalSeconds, projects))
        }

        weeklyData.reverse()
        return weeklyData.toList()
    }

    @Composable
    private fun MyContent() {
        val context = LocalContext.current
        val processedData = WakaDataWorker.loadProcessedData(context)
        println("The processed data is $processedData")

        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        // create a graph mode state
        var graphMode = remember { mutableStateOf(GraphMode.Daily) }

        val dailyData = generateDailyData(processedData)
        val weeklyData = generateWeeklyData(processedData)

        val targetInHours =
            when (graphMode.value) {
                GraphMode.Daily -> prefs.getFloat(WakaHelpers.DAILY_TARGET_HOURS, 0f)
                GraphMode.Weekly -> prefs.getFloat(WakaHelpers.WEEKLY_TARGET_HOURS, 0f)
            }

        val maxHours =
            when (graphMode.value) {
                GraphMode.Daily -> 24 * TIME_WINDOW_PROPORTION
                GraphMode.Weekly -> 24 * 7 * TIME_WINDOW_PROPORTION
            }

        val streak = WakaDataWorker.loadStreak(context, graphMode.value);

        Column(
            modifier = GlanceModifier.fillMaxSize(),
//                .background(Color.Black),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // top bar
            Row(
                modifier = GlanceModifier.fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = streak.toString(),
                    style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White))
                )
                Box(modifier = GlanceModifier.defaultWeight()) {}
                Row {
                    Text(
                        text = "DAILY",
                        style = TextStyle(
                            color = when (graphMode.value) {
                                GraphMode.Daily -> ColorProvider(
                                    day = Color.White,
                                    night = Color.White
                                )

                                GraphMode.Weekly -> ColorProvider(
                                    day = Color.Gray,
                                    night = Color.Gray
                                )
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

                                GraphMode.Weekly -> ColorProvider(
                                    day = Color.White,
                                    night = Color.White
                                )
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
                        .fillMaxWidth().fillMaxHeight().padding(bottom = GRAPH_BOTTOM_PADDING.dp),
                ) {
                    // map all days or weeks depending on graph mode
                    when (graphMode.value) {
                        GraphMode.Daily -> dailyData
                        GraphMode.Weekly -> weeklyData
                    }.forEach {
                        Column(
                            modifier = GlanceModifier
//                            .background(Color.Green)
                                .width(50.dp).padding(horizontal = 3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            var barColor = Color.Gray
                            if (it.totalSeconds / 3600 >= targetInHours) {
                                barColor = Color.Black
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
                                        color = ColorProvider(
                                            day = Color.White,
                                            night = Color.White
                                        )
                                    )
                                )
                            }

                        }
                    }
                }

                // Add this Box for the target line overlay
                TargetLine(targetInHours, maxHours)
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

    @Composable
    private fun TargetLine(targetHours: Float, maxHours: Float) {
        // Target line - positioned at a specific height from bottom
        // For example, if your target is 4 hours (3600*4 seconds)
        val targetHeight = (GRAPH_HEIGHT * min(
            1f,
            (3600 * targetHours) / (3600 * maxHours)
        ) + GRAPH_BOTTOM_PADDING + DATE_TEXT_HEIGHT).dp
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(bottom = targetHeight),

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
                        modifier = GlanceModifier.fillMaxSize().background(Color.LightGray)
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
