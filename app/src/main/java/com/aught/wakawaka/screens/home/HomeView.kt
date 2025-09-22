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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.StrokeCap
import org.koin.androidx.compose.koinViewModel

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
    ogProjectId: String?,
    viewModel: HomeViewModel = koinViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(ogProjectId) {
        if (ogProjectId != null) {
            viewModel.selectProject(ogProjectId)
        } else {
            viewModel.selectProject(WakaHelpers.ALL_PROJECTS_ID)
        }
    }

    val projects by viewModel.projects.collectAsState()
    val aggregateData by viewModel.aggregateData.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.unloaded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {
//            CircularProgressIndicator(
//                modifier = Modifier
//                    .fillMaxWidth(0.4f),
////                    .height(16.dp),
//                strokeCap = StrokeCap.Round,
////                gapSize = 12.dp
//                strokeWidth = 12.dp
//            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp),
                strokeCap = StrokeCap.Round,
                gapSize = 12.dp
//                strokeWidth = 12.dp
            )
        }
        return
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
                        uiState.selectedProjectName,
                        listOf(WakaHelpers.ALL_PROJECTS_ID) + projects.map { it.name }
                    ) {
                        viewModel.selectProject(it)
                    }

                    Text(
                        text = WakaHelpers.durationInSecondsToDurationString(
                            uiState.durationLabelValueMap["All Time"] ?: 0
                        ),
                        fontSize = 24.sp
                    )
                }
                Box(
                    modifier = Modifier
                ) {
                    ShareButton(
                        context,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = (-12).dp, x = 18.dp)
                    )
                    Box(
                        modifier = Modifier.offset(y = 12.dp, x = -8.dp)
                    ) {
                        DailyStreakDisplay()
//                        if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
//                            AggregateStreakDisplay(wakaDataHandler)
//                        } else {
//                            ProjectStreakDisplay(selectedProject, wakaDataHandler)
//                        }
                    }
                }
            }

            // ? ........................
            // endregion ........................

            val primaryColor = MaterialTheme.colorScheme.primary

            CalendarGraph(
                projectName = uiState.selectedProjectName,
                uiState.dateToDurationMap,
                targetInHours = uiState.dailyTargetStreakData.target,
                projectColor = uiState.projectColor ?: primaryColor,
                aggregateData
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                uiState.durationLabelValueMap.forEach { (timeRange, durationInSeconds) ->
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
                text = WakaHelpers.truncateLabel(selectedProject, 16),
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

