package com.aught.wakawaka.screens

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DurationStats
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.utils.ColorUtils
import java.time.LocalDate
import kotlin.math.min


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView() {
    var selectedProject by remember { mutableStateOf(WakaHelpers.ALL_PROJECTS_ID) }

    val dataRequest = if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) DataRequest.Aggregate else DataRequest.ProjectSpecific(selectedProject)

    val context = LocalContext.current

    val aggregateData = WakaDataFetchWorker.loadAggregateData(context)
    val projectSpecificData = WakaDataFetchWorker.loadProjectSpecificData(context)
    val wakaStatistics = WakaDataFetchWorker.loadWakaStatistics(context)

    val wakaDataHandler = WakaDataHandler(aggregateData, projectSpecificData)

    val projects = mutableListOf(WakaHelpers.ALL_PROJECTS_ID)
    projects.addAll(wakaDataHandler.getSortedProjectList())


    val durationStats: DurationStats = if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
        wakaStatistics.aggregateStats
    } else {
        wakaStatistics.projectStats[selectedProject] ?: DurationStats(0, 0, 0, 0, 0)
    }

    val durationLabelValueMap = mapOf(
        "Today" to durationStats.today,
        "This Week" to wakaDataHandler.getOffsetPeriodicDurationInSeconds(dataRequest, TimePeriod.WEEK, 0),
//        "Last 7 Days" to durationStats.last7Days,
        "Past 30 Days" to durationStats.last30Days,
        "Past Year" to durationStats.lastYear,
    )

    val dateToDurationMap = wakaDataHandler.getDateToDurationData(dataRequest)

    val dailyTargetHit = wakaDataHandler.targetHit(dataRequest, TimePeriod.DAY)

    val streakCount = wakaDataHandler.getStreak(dataRequest, TimePeriod.DAY).count + (if (dailyTargetHit) 1 else 0)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
//                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProjectSelector(
                    selectedProject, projects
                ) {
                    selectedProject = it
                }

                Text(
                    text = WakaHelpers.durationInSecondsToDurationString(durationStats.allTime),
                    fontSize = 24.sp
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = streakCount.toString(),
                    fontSize = 72.sp,
                    color = if (dailyTargetHit) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    },
                    fontWeight = FontWeight.ExtraBold,
                )
//                Column(
//                    modifier = Modifier.padding(start = 2.dp, bottom = 20.dp),
//                    verticalArrangement = Arrangement.spacedBy(4.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                ) {
//                    val streakColors = ColorUtils.getStreakColors(streakCount, 0.3f)
//                    streakColors.forEach {
//                        Box(
//                            modifier = Modifier
//                                .size(15.dp)
//                                .clip(RoundedCornerShape(corner = CornerSize(5.dp)))
//                                .background(it)
//                        ) {}
//                    }
//                    if (dailyTargetHit) {
//                        Box(
//                            modifier = Modifier
//                                .width(15.dp)
//                                .height(5.dp)
//                                .clip(RoundedCornerShape(corner = CornerSize(5.dp)))
//                                .background(MaterialTheme.colorScheme.onSurface)
//                        ) {}
//                    }
//                }
            }
        }
        CalendarGraph(
            dateToDurationMap.mapValues {
                // divide by the target hours to get the percentage
                if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
                    if (aggregateData?.dailyTargetHours == null) {
                        if (it.value > 0) 1f else 0f
                    } else {
                        min(1f, it.value.toFloat() / (aggregateData.dailyTargetHours * 3600))
                    }
                } else {
                    if (projectSpecificData[selectedProject]?.dailyTargetHours == null) {
                        if (it.value > 0) 1f else 0f
                    } else {
                        min(1f, it.value.toFloat() / (projectSpecificData[selectedProject]!!.dailyTargetHours!! * 3600f))
                    }
                }
            },
            projectColor = if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
                MaterialTheme.colorScheme.primary
            } else {
                if (projectSpecificData[selectedProject]?.color == null) {
                    WakaHelpers.projectNameToColor(selectedProject)
                } else {
                    runCatching { Color(projectSpecificData[selectedProject]!!.color.toColorInt()) }.getOrNull() ?: WakaHelpers.projectNameToColor(
                        selectedProject
                    )
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            durationLabelValueMap.forEach { (timeRange, durationInSeconds) ->
                DurationStatView(timeRange, durationInSeconds)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationStatView(timeRange: String, durationInSeconds: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    )
    {
        val durationString = WakaHelpers.durationInSecondsToDurationString(durationInSeconds)

        Text(text = timeRange, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(text = durationString, fontSize = 18.sp)
    }
}

data class DayData(
    val date: Int,
    val month: String,
    val day: String,
    val duration: Float,
    val isFutureDate: Boolean = false,
    val isToday: Boolean = false
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekGraph(data: List<DayData>, textColor: Color, projectColor: Color) {
    val cellSize = 48.dp

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        (0..6).forEach {
            Card(
                modifier = Modifier
                    .size(cellSize)
                    .padding(3.dp)
            ) {
                val bgColor = projectColor.copy(
                    0.1f + (0.9f * data[it].duration)
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isFirstOfMonth = data[it].date == 1
                    val isToday = data[it].isToday
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = if (isToday) {
                            Modifier
                                .fillMaxSize(0.6f)
                                .border(1.dp, Color.White, CircleShape)
                        } else {
                            Modifier.fillMaxSize()
                        }

                    ) {
                        Text(
                            text = if (isFirstOfMonth) data[it].month.slice(0..2) else data[it].date.toString(),
//                        textDecoration = if (data[it].isToday) TextDecoration.Underline else null,
                            color = (
                                    if (data[it].isFutureDate) {
                                        textColor.copy(0.2f)
                                    } else {
                                        textColor.copy(if (isToday) 1f else 0.7f)
                                    }
                                    ),
                            fontSize = when {
                                isFirstOfMonth -> 12.sp
                                data[it].isToday -> 16.sp
                                else -> 14.sp
                            },
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGraph(dateToDurationMap: Map<String, Float>, projectColor: Color) {
    val numWeeks = 16
    val scrollState = rememberScrollState()

    val today = LocalDate.now()

    // move back to monday
    val startDate = today.minusDays((today.dayOfWeek.value - 1).toLong())

    val textColor = ColorUtils.getContrastingTextColor(projectColor)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(fraction = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState),
        ) {
            (0..numWeeks).forEach {
                val weekData = emptyList<DayData>().toMutableList()
                val firstDateOfWeek = startDate.minusWeeks(it.toLong())
                (0..6).forEach { day ->
                    // for each day of the week, generate the date string and get the duration
                    val date = firstDateOfWeek.plusDays(day.toLong())
                    val dateString = date.format(WakaHelpers.getYYYYMMDDDateFormatter())
                    val duration = dateToDurationMap[dateString] ?: 0f
                    val dayData = DayData(
                        date.dayOfMonth,
                        date.month.toString(),
                        date.dayOfWeek.toString(),
                        duration,
                        isFutureDate = date.isAfter(today),
                        isToday = date.isEqual(today)
                    )
                    weekData.add(dayData)
                }

                WeekGraph(weekData, textColor, projectColor)
            }
        }

        if (scrollState.value != 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        if (scrollState.value != scrollState.maxValue) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelector(
    selectedProject: String,
    projects: List<String>,
    onSelectProject: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = selectedProject, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(200.dp)
                .verticalScroll(rememberScrollState())
                .heightIn(max = 300.dp)
        ) {
            projects.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectProject(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

