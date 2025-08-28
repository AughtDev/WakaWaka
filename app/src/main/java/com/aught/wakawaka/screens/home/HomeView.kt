package com.aught.wakawaka.screens.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DurationStats
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.aught.wakawaka.data.WakaHelpers
import android.util.Log
import androidx.compose.foundation.layout.Arrangement

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
fun HomeView(
    ogProjectId: String?
) {
    val context = LocalContext.current

    var selectedProject by remember { mutableStateOf(ogProjectId ?: WakaHelpers.ALL_PROJECTS_ID) }

    val dataRequest by remember {
        derivedStateOf {
            if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) DataRequest.Aggregate else DataRequest.ProjectSpecific(
                selectedProject
            )
        }
    }

    var aggregateData by remember {
        mutableStateOf(WakaDataFetchWorker.loadAggregateData(context))
    }
    var projectSpecificData by remember {
        mutableStateOf(WakaDataFetchWorker.loadProjectSpecificData(context))
    }
    var wakaStatistics by remember {
        mutableStateOf(WakaDataFetchWorker.loadWakaStatistics(context))
    }

    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        Log.d("waka", "SharedPreferences changed: $key")
        when (key) {
            WakaHelpers.AGGREGATE_DATA_KEY -> {
                aggregateData = WakaDataFetchWorker.loadAggregateData(context)
            }

            WakaHelpers.PROJECT_SPECIFIC_DATA_KEY -> {
                projectSpecificData = WakaDataFetchWorker.loadProjectSpecificData(context)
            }

            WakaHelpers.WAKA_STATISTICS_KEY -> {
                wakaStatistics = WakaDataFetchWorker.loadWakaStatistics(context)
            }
        }
    }

    DisposableEffect(prefs) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val wakaDataHandler by remember {
        derivedStateOf { WakaDataHandler(aggregateData, projectSpecificData) }
    }

    val projects = remember {
        mutableListOf(WakaHelpers.ALL_PROJECTS_ID).apply {
            addAll(wakaDataHandler.sortedProjectList)
        }
    }


    val durationLabelValueMap by remember {
        derivedStateOf {
            val durationStats: DurationStats = if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
                wakaStatistics.aggregateStats
            } else {
                wakaStatistics.projectStats[selectedProject] ?: DurationStats(0, 0, 0, 0, 0)
            }

            mapOf(
                "Today" to durationStats.today,
                "This Week" to wakaDataHandler.getOffsetPeriodicDurationInSeconds(
                    dataRequest,
                    TimePeriod.WEEK,
                    0
                ),
//        "Last 7 Days" to durationStats.last7Days,
                "Past 30 Days" to durationStats.last30Days,
                "Past Year" to durationStats.lastYear,
                "All Time" to durationStats.allTime
            )
        }
    }

    val dateToDurationMap by remember {
        derivedStateOf {
            Log.d("waka", "Getting date to duration map for: $selectedProject")
            wakaDataHandler.getDateToDurationData(dataRequest)
        }
    }


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
            Log.d("waka", "Header: $selectedProject")
            // region HEADER
            // ? ........................

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProjectSelector(
                        selectedProject, projects
                    ) {
                        selectedProject = it
                    }

                    Text(
                        text = WakaHelpers.durationInSecondsToDurationString(durationLabelValueMap["All Time"] ?: 0),
                        fontSize = 24.sp
                    )
                }
//                Text(
//                    text = streakCountAndTargetHit.first.toString(),
//                    fontSize = 72.sp,
//                    color = if (streakCountAndTargetHit.second) {
//                        MaterialTheme.colorScheme.onSurface
//                    } else {
//                        MaterialTheme.colorScheme.onSurface.copy(0.5f)
//                    },
//                    fontWeight = FontWeight.Bold,
//                )
                if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
                    AggregateStreakDisplay(wakaDataHandler)
                } else {
                    val projectData = projectSpecificData[selectedProject]
                    if (projectData != null) {
                        ProjectStreakDisplay(projectData, wakaDataHandler)
                    }
                }
            }

            // ? ........................
            // endregion ........................

            val targetInHours by remember {
                derivedStateOf {
                    if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
                        aggregateData?.dailyTargetHours
                    } else {
                        projectSpecificData[selectedProject]?.dailyTargetHours
                    }
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary

            val projectColor by remember {
                derivedStateOf {
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
            }

            CalendarGraph(
                projectName = selectedProject,
                dateToDurationMap,
                targetInHours = targetInHours,
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
                    // show all time at the top of the page, not here
                    if (timeRange != "All Time") {
                        DurationStatView(timeRange, durationInSeconds)
                    }
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
                text = WakaHelpers.truncateLabel(selectedProject,16),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
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
                    text = {
                        Text(WakaHelpers.truncateLabel(option, 25))
                    },
                    onClick = {
                        onSelectProject(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

