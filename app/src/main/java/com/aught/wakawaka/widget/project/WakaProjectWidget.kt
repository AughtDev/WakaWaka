package com.aught.wakawaka.widget.project

import android.content.Context
import androidx.glance.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaWidgetTheme
import com.aught.wakawaka.utils.ColorUtils
import com.aught.wakawaka.widget.WakaWidgetComponents
import com.aught.wakawaka.widget.WakaWidgetHelpers
import kotlin.math.min

class WakaProjectWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MyContent(context)
        }
    }

    @Composable
    private fun MyContent(context: Context) {
//        val context = LocalContext.current
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
        val widgetProject = prefs.getString(WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET, null)

        println("rendering project widget again")

        if (widgetProject == null) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No project assigned",
                )
            }
        } else {
            val wakaDataHandler = WakaDataHandler.fromContext(context)
            val dataRequest = DataRequest.ProjectSpecific(widgetProject)

            // create a graph mode state
            var graphMode = remember { mutableStateOf(GraphMode.Daily) }
            val timePeriod = when (graphMode.value) {
                GraphMode.Daily -> TimePeriod.DAY
                GraphMode.Weekly -> TimePeriod.WEEK
            }

            val data = wakaDataHandler.getPeriodicDurationsInSeconds(dataRequest, timePeriod, 7)
            val dates = wakaDataHandler.getPeriodicDates(dataRequest, timePeriod, 7)

            val targetInHours = wakaDataHandler.getTarget(dataRequest, timePeriod)

            val maxHours =
                when (graphMode.value) {
                    GraphMode.Daily -> 24 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
                    GraphMode.Weekly -> 24 * 7 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
                }

            val streak = wakaDataHandler.getStreak(dataRequest, timePeriod).count

            val hitTargetToday = wakaDataHandler.targetHit(dataRequest, timePeriod)

            val excludedDays = wakaDataHandler.getExcludedDays(dataRequest, timePeriod)

            val theme = when (prefs.getInt(WakaHelpers.THEME, 0)) {
                0 -> WakaWidgetTheme.Light
                1 -> WakaWidgetTheme.Dark
                else -> WakaWidgetTheme.Dark
            }

            val projectColor = wakaDataHandler.getProjectColor(widgetProject)

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
                    }).copy(alpha = 0.3f)
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
                            text = WakaHelpers.truncateLabel(widgetProject).uppercase(),
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
                            dates.zip(data).forEach {
                                val date = it.first
                                val duration = it.second

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
                                            date.dayOfWeek.value in excludedDays ||
                                            duration > (targetInHours * 3600)
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
                                                        duration / (3600 * maxHours)
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

