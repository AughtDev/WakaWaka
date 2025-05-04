package com.aught.wakawaka.screens

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aught.wakawaka.data.DurationStats
import com.aught.wakawaka.data.StreakData
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView() {
    val context = LocalContext.current
    val aggregateData = WakaDataWorker.loadAggregateData(context)
    val projectSpecificData = WakaDataWorker.loadProjectSpecificData(context)
    val wakaStatistics = WakaDataWorker.loadWakaStatistics(context)

    println("WakaStats ${projectSpecificData["WakaWaka"]}")

    val projects = mutableListOf(WakaHelpers.ALL_PROJECTS_ID)
    projects.addAll(projectSpecificData.map { it.value.name })

    var selectedProject by remember { mutableStateOf(WakaHelpers.ALL_PROJECTS_ID) }

    val streak =
        (if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) aggregateData?.dailyStreak else projectSpecificData[selectedProject]?.dailyStreak)
            ?: StreakData(0, WakaHelpers.ZERO_DAY)

    val durationStats: DurationStats = if (selectedProject == WakaHelpers.ALL_PROJECTS_ID) {
        wakaStatistics.aggregateStats
    } else {
        wakaStatistics.projectStats[selectedProject] ?: DurationStats(0, 0, 0, 0, 0)
    }

    val durationLabelValueMap = mapOf(
        "Today" to durationStats.today,
        "Last 7 Days" to durationStats.last7Days,
        "Last 30 Days" to durationStats.last30Days,
        "Last Year" to durationStats.lastYear,
    )

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

                Text(text = "5h 37m", fontSize = 24.sp)
            }
            Text(
                text = streak.count.toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        CalendarGraph()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekGraph() {
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
            ) {}

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGraph() {
    val numWeeks = 16
    val scrollState = rememberScrollState()

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
                WeekGraph()
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
            onDismissRequest = { expanded = false }
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

