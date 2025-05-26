package com.aught.wakawaka.screens.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DurationStats
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.utils.ColorUtils
import java.time.LocalDate
import kotlin.math.min
import android.util.Log

fun refreshWakaData(context: Context, setIsLoading: ((Boolean) -> Unit)) {
    Log.d("waka", "Refreshing Waka data...")

    val workManagerInstance = WorkManager.getInstance(context)
    // Schedule the one time immediate worker
    val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataFetchWorker>().build()

    val observer = object : Observer<WorkInfo?> {
        override fun onChanged(workInfo: WorkInfo?) {
            if (workInfo != null && workInfo.state.isFinished) {
                WorkManager.getInstance(context).getWorkInfoByIdLiveData(immediateWorkRequest.id)
                    .removeObserver(this)
                setIsLoading(false)
                Log.d("waka", "Waka data refreshed successfully.")
            }
        }
    }

    workManagerInstance.enqueue(immediateWorkRequest)
    workManagerInstance.getWorkInfoByIdLiveData(immediateWorkRequest.id)
        .observeForever(observer)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView() {
    val context = LocalContext.current

    var refreshKey by remember { mutableStateOf(0) }
    var selectedProject by remember { mutableStateOf(WakaHelpers.ALL_PROJECTS_ID) }

    val dataRequest =
        if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) DataRequest.Aggregate else DataRequest.ProjectSpecific(
            selectedProject
        )

    val aggregateData = remember(refreshKey) {
        WakaDataFetchWorker.loadAggregateData(context)
    }
    val projectSpecificData = remember(refreshKey) {
        WakaDataFetchWorker.loadProjectSpecificData(context)
    }
    val wakaStatistics = remember(refreshKey) {
        WakaDataFetchWorker.loadWakaStatistics(context)
    }
    Log.d("waka", "HomeView: refreshKey = $refreshKey, selectedProject = $selectedProject")

    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        Log.d("waka", "SharedPreferences changed: $key")
        when (key) {
            WakaHelpers.AGGREGATE_DATA -> {
                refreshKey++
            }

            WakaHelpers.PROJECT_SPECIFIC_DATA -> {
                refreshKey++
            }

            WakaHelpers.WAKA_STATISTICS -> {
                refreshKey++
            }
        }
    }

    DisposableEffect(prefs) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

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
        "This Week" to wakaDataHandler.getOffsetPeriodicDurationInSeconds(
            dataRequest,
            TimePeriod.WEEK,
            0
        ),
//        "Last 7 Days" to durationStats.last7Days,
        "Past 30 Days" to durationStats.last30Days,
        "Past Year" to durationStats.lastYear,
    )

    val dateToDurationMap = remember {
        wakaDataHandler.getDateToDurationData(dataRequest)
    }

    val dailyTargetHit = remember {
        wakaDataHandler.targetHit(dataRequest, TimePeriod.DAY)
    }

    val streakCount = wakaDataHandler.getStreak(
        dataRequest,
        TimePeriod.DAY
    ).count + (if (dailyTargetHit) 1 else 0)

    var isRefreshingData by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshingData,
        onRefresh = {
            isRefreshingData = true
            refreshWakaData(context) {
                isRefreshingData = it
            }
        }
    ) {
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
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(
                        contentAlignment = Alignment.TopStart,
                        modifier = Modifier
                            .height(64.dp)
                            .width(48.dp)
                    ) {
                        //                    IconButton(
                        //                        {
                        //                            val workManagerInstance = WorkManager.getInstance(context)
                        //                            // Schedule the one time immediate worker
                        //                            val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataFetchWorker>().build()
                        //
                        //                            val observer = object : Observer<WorkInfo?> {
                        //                                override fun onChanged(workInfo: WorkInfo?) {
                        //                                    if (workInfo != null && workInfo.state.isFinished) {
                        //                                        WorkManager.getInstance(context).getWorkInfoByIdLiveData(immediateWorkRequest.id)
                        //                                            .removeObserver(this)
                        //                                        reloadKey++
                        //                                    }
                        //                                }
                        //                            }
                        //
                        //                            workManagerInstance.enqueue(immediateWorkRequest)
                        //                            workManagerInstance.getWorkInfoByIdLiveData(immediateWorkRequest.id)
                        //                                .observeForever(observer)
                        //                        },
                        //                    ) {
                        //                        Icon(
                        //                            Icons.Default.Refresh,
                        //                            contentDescription = "Refresh Data",
                        //                            modifier = Modifier
                        //                                .size(32.dp)
                        //                                .fillMaxHeight()
                        //                        )
                        //                    }
                    }
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
                }
            }
            val targetSeconds = remember {
                if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
                    aggregateData?.dailyTargetHours?.times(3600) ?: 0f
                } else {
                    projectSpecificData[selectedProject]?.dailyTargetHours?.times(3600) ?: 0f
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val projectColor = remember(selectedProject, projectSpecificData) {
                if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
                    primaryColor
                } else {
                    if (projectSpecificData[selectedProject]?.color == null) {
                        WakaHelpers.projectNameToColor(selectedProject)
                    } else {
                        runCatching { Color(projectSpecificData[selectedProject]!!.color.toColorInt()) }.getOrNull()
                            ?: WakaHelpers.projectNameToColor(selectedProject)
                    }
                }
            }

            CalendarGraph(
                projectName = selectedProject,
                dateToDurationMap,
                targetSeconds = targetSeconds,
                projectColor = projectColor,
                aggregateData
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
    val year: String,
    val yyyymmdd: String,
    val duration: Float,
    val isFutureDate: Boolean = false,
    val isToday: Boolean = false
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekGraph(
    data: List<DayData>,
    textColor: Color,
    projectColor: Color,
    excludedDays: Set<Int>,
    setDialogDayData: (DayData) -> Unit
) {
    val cellSize = 48.dp

    val bgLuminance = ColorUtils.calculateLuminance(MaterialTheme.colorScheme.background)

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        (0..6).forEach {
            val isFirstOfMonth = data[it].date == 1
            val isToday = data[it].isToday

            val cardModifier = Modifier
                .size(cellSize)
                .padding(3.dp)
            Card(
//                modifier = if (isFirstOfMonth) cardModifier.border(2.dp, Color.Black, RoundedCornerShape(8.dp)) else cardModifier
                modifier = cardModifier.clickable(onClick = {
                    setDialogDayData(data[it])
                })
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
                    val luminance =
                        WakaHelpers.PROJECT_COLOR_LUMINANCE * (data[it].duration) + bgLuminance * (1 - data[it].duration)
                    val customTextColor = if (isFirstOfMonth || luminance < 0.4f) {
                        projectColor
                    } else {
                        if (data[it].isFutureDate) {
                            textColor.copy(0.2f)
                        } else {
                            textColor.copy(if (isToday) 1f else 0.7f)
                        }
                    }
                    if (!excludedDays.contains(it) && !data[it].isFutureDate) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = if (isToday && !isFirstOfMonth) {
                                Modifier
                                    .fillMaxSize(0.6f)
                                    .border(1.dp, customTextColor, CircleShape)
                            } else {
                                Modifier.fillMaxSize()
                            }

                        ) {
                            Text(
                                text = if (isFirstOfMonth) data[it].month.slice(0..2) else data[it].date.toString(),
                                color = customTextColor,
                                fontSize = when {
                                    isFirstOfMonth -> 12.sp
                                    data[it].isToday -> 16.sp
                                    else -> 14.sp
                                },
                                textDecoration = if (isFirstOfMonth && isToday) TextDecoration.Underline else TextDecoration.None,
                                modifier = if (isFirstOfMonth && !data[it].isFutureDate) Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 3.dp, vertical = 2.dp) else Modifier,
                                fontWeight = if (isFirstOfMonth) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun generateWeeklyData(
    dateToDurationMap: Map<String, Int>,
    targetSeconds: Float,
    minWeeks: Int = 8
): List<List<DayData>> {
    // first get the earliest date and calculate the number of weeks
    val numWeeks = dateToDurationMap.let {
        if (it.isEmpty()) {
            minWeeks
        } else {
            val dates = it.keys.map { WakaHelpers.yyyyMMDDToDate(it) }
            val minDate = dates.minOrNull() ?: LocalDate.now()
            val weeks = (LocalDate.now().toEpochDay() - minDate.toEpochDay()) / 7
            maxOf(weeks.toInt(), minWeeks)
        }
    }

    val weeklyData = mutableListOf<List<DayData>>()

    // move back to monday
    val today = LocalDate.now()
    val startDate = today.minusDays((today.dayOfWeek.value - 1).toLong())

    (0..numWeeks).forEach {
        val weekData = emptyList<DayData>().toMutableList()
        val firstDateOfWeek = startDate.minusWeeks(it.toLong())
        (0..6).forEach { day ->
            // for each day of the week, generate the date string and get the duration
            val date = firstDateOfWeek.plusDays(day.toLong())
            val dateString = date.format(WakaHelpers.getYYYYMMDDDateFormatter())
            val duration = dateToDurationMap[dateString] ?: 0
            val dayData = DayData(
                date.dayOfMonth,
                date.month.toString(),
                date.dayOfWeek.toString(),
                date.year.toString(),
                dateString,
//                if (targetSeconds == 0f) {
//                    if (duration > 0) 1f else 0f
//                } else {
//                    min(1f, duration / targetSeconds)
//                },
                min(1f, duration / (if (targetSeconds == 0f) 3600f else targetSeconds)),
                isFutureDate = date.isAfter(today),
                isToday = date.isEqual(today)
            )
            weekData.add(dayData)
        }
        weeklyData.add(weekData)
    }

    return weeklyData;
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGraph(
    projectName: String,
    dateToDurationMap: Map<String, Int>,
    targetSeconds: Float,
    projectColor: Color,
    aggregateData: AggregateData? = null
) {
    Log.d("waka", "Generating calendar graph for project: $projectName")
    var showDialog by remember { mutableStateOf(false) }
    var dialogDayData by remember { mutableStateOf<DayData?>(null) }

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                dialogDayData = null
            }
        ) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // date string of dialog day data of the format 15th May 2025
                val dateString = dialogDayData?.let {
                    "${it.date}${WakaHelpers.getDateSuffix(it.date)} ${it.month} ${it.year}"
                } ?: "No date selected"
                val durationString = dialogDayData?.let {
                    WakaHelpers.durationInSecondsToDurationString(
                        dateToDurationMap[it.yyyymmdd] ?: 0, "h", "m"
                    )
                } ?: "No duration selected"
                val projects = dialogDayData?.let {
                    if (projectName != WakaHelpers.ALL_PROJECTS_ID) {
                        null
                    } else {
                        aggregateData?.dailyRecords[it.yyyymmdd]?.projects
                    }
                }?.sortedByDescending { it.totalSeconds }
                val dayString = dialogDayData?.day?.uppercase() ?: "No day selected"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollable(rememberScrollState(), Orientation.Vertical),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(dayString, fontSize = 16.sp)
                    Text(dateString)
                    if (projects != null) {
                        val textSize = 12.sp
                        val padding = 8.dp
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                projects.forEach {
                                    Text(WakaHelpers.truncateLabel(it.name.uppercase(), 16), fontSize = textSize)
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = padding),
                                horizontalAlignment = Alignment.End
                            ) {
                                projects.forEach {
                                    Text(WakaHelpers.durationInSecondsToDurationString(it.totalSeconds), fontSize = textSize)
                                }
                            }
                        }
                    }
                    Text(durationString)
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val weeklyData = generateWeeklyData(dateToDurationMap, targetSeconds)
    val textColor = ColorUtils.getContrastingTextColor(projectColor)

    val setDialogDayData = { it: DayData ->
        dialogDayData = it
        showDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(fraction = 0.65f)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxHeight()
        ) {
            items(weeklyData) {
                var firstJanIdx: Int? = null
                it.forEachIndexed { index, it ->
                    if (it.date == 1 && it.month.uppercase() == "JANUARY") {
                        firstJanIdx = index
                        return@forEachIndexed
                    }
                }
                if (firstJanIdx != null) {
                    WeekGraph(
                        it,
                        textColor,
                        projectColor,
                        if (firstJanIdx != 0) (0..<firstJanIdx!!).toSet() else emptySet(),
                        setDialogDayData
                    )
                    // display the new year
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it.lastOrNull()?.year?.substring(0, 4) ?: "NEW YEAR",
                            fontSize = 24.sp,
                            color = projectColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (firstJanIdx != 0) {
                        WeekGraph(
                            it,
                            textColor,
                            projectColor,
                            (firstJanIdx!!..7).toSet(),
                            setDialogDayData
                        )
                    }
                } else {
                    WeekGraph(it, textColor, projectColor, emptySet(), setDialogDayData)
                }
            }
        }
        val firstVisibleItemIndex = remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex
            }
        }

        val layoutInfo = remember { derivedStateOf { lazyListState.layoutInfo } }

        if (layoutInfo.value.viewportStartOffset > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        80.dp
//                        min(
//                            80,
//                            (layoutInfo.value.viewportStartOffset)
//                        ).dp
                    )
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
        if (layoutInfo.value.visibleItemsInfo.lastOrNull()?.index != weeklyData.lastIndex) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(
                        // height depends on how close the last item is to the bottom of the screen
                        80.dp
//                        min(
//                            80.dp,
//                            (layoutInfo.value.viewportSize.height - layoutInfo.value.visibleItemsInfo.lastOrNull().offset)
//                        )
                    )
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
            Text(
                text = WakaHelpers.truncateLabel(selectedProject),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
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

