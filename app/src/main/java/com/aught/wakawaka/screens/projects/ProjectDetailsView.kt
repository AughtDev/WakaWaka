package com.aught.wakawaka.screens.projects

import AlertData
import DailyTargetCard
import AlertPane
import WeeklyTargetCard
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import androidx.navigation.NavHostController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aught.wakawaka.Screen
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.StreakData
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.workers.WakaProjectWidgetUpdateWorker
import kotlinx.coroutines.delay
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsView(projectName: String, navController: NavHostController) {
    val context = LocalContext.current
    val projectSpecificData = WakaDataFetchWorker.loadProjectSpecificData(context)
    val projectData = projectSpecificData[projectName]

    if (projectData != null) {
        var alertData by remember { mutableStateOf<AlertData?>(null) }

        var withDailyTarget by remember {
            mutableStateOf(
                projectData.dailyTargetHours != null
            )
        }
        var dailyTarget by remember {
            mutableStateOf(projectData.dailyTargetHours ?: 2f)
        }

        var withWeeklyTarget by remember {
            mutableStateOf(
                projectData.weeklyTargetHours != null
            )
        }
        var weeklyTarget by remember {
            mutableStateOf(projectData.weeklyTargetHours ?: 10f)
        }

        var dailyStreakExcludedDays by remember {
            mutableStateOf(
                projectData.excludedDaysFromDailyStreak
            )
        }

        LaunchedEffect(alertData) {
            if (alertData != null) {
                // Hide the success message after 2 seconds
                delay(2000)
                alertData = null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(36.dp),
                    // go back to the projects screen
                    onClick = {
                        navController.navigate(Screen.Projects.route) {
                            popUpTo(Screen.Projects.route) {
                                inclusive = true
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Go back to projects",
                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = projectData.name,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }


            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
//                ProjectStreakCards(projectData)
                ProjectStats(context, projectName)
                DailyTargetCard(
                    dailyTarget = dailyTarget,
                    withDailyTarget = withDailyTarget,
                    dailyStreakExcludedDays = dailyStreakExcludedDays,
                    onDailyTargetChange = { dailyTarget = it },
                    onWithDailyTargetChange = { withDailyTarget = it },
                    onExcludedDaysChange = { dailyStreakExcludedDays = it }
                )

                // Weekly Target Section

                WeeklyTargetCard(
                    weeklyTarget = weeklyTarget,
                    withWeeklyTarget = withWeeklyTarget,
                    onWeeklyTargetChange = { weeklyTarget = it },
                    onWithWeeklyTargetChange = { withWeeklyTarget = it }
                )

                // Success message
                AlertPane(
                    alertData,
//                    AlertData("$projectName settings saved successfully!")
                    alertData != null
                )


                // Save Button
                Button(
                    onClick = {
                        WakaDataFetchWorker.saveProjectData(
                            context, projectName, ProjectSpecificData(
                                projectData.name,
                                projectData.color,
                                projectData.dailyDurationInSeconds,
                                if (withDailyTarget) dailyTarget else null,
                                if (withWeeklyTarget) weeklyTarget else null,
                                StreakData(0, WakaHelpers.ZERO_DAY),
                                StreakData(0, WakaHelpers.ZERO_DAY),
                                dailyStreakExcludedDays,
                            )
                        )
                        val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaProjectWidgetUpdateWorker>().build()
                        val workerInstance = WorkManager.getInstance(context)

                        workerInstance.enqueue(immediateWorkRequest)
                        val observer = object : Observer<WorkInfo?> {
                            override fun onChanged(workInfo: WorkInfo?) {
                                if (workInfo?.state?.isFinished == true) {
                                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                        alertData = AlertData("$projectName settings saved successfully!")
                                        workerInstance.getWorkInfoByIdLiveData(immediateWorkRequest.id).removeObserver(this)
                                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                                        alertData = AlertData("Failed to save $projectName settings", AlertType.Failure)
                                        workerInstance.getWorkInfoByIdLiveData(immediateWorkRequest.id).removeObserver(this)
                                    }
                                }
                            }
                        }
                        workerInstance.getWorkInfoByIdLiveData(immediateWorkRequest.id).observeForever(observer)

                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save $projectName Settings")
                }
            }
        }
    } else {
        // Handle the case where project data is not found
        Text(text = "Project data not found")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectStreakCards(projectData: ProjectSpecificData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Daily Streak", fontSize = 10.sp)
                Text(
                    ((projectData.dailyStreak?.count ?: 0) + if (WakaDataFetchWorker.dailyTargetHit(
                            projectData.dailyDurationInSeconds,
                            projectData.dailyTargetHours,
                        )
                    ) 1 else 0).toString(),
                    fontSize = 32.sp
                )
            }

        }
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Weekly Streak", fontSize = 10.sp)
                Text(
                    ((projectData.weeklyStreak?.count ?: 0) + if (WakaDataFetchWorker.weeklyTargetHit(
                            projectData.dailyDurationInSeconds,
                            projectData.weeklyTargetHours
                        )
                    ) 1 else 0).toString(),
                    fontSize = 32.sp
                )
            }

        }
    }
}

@Composable
fun ProjectStats(context: Context, projectName: String) {
    val projectStats = WakaDataFetchWorker.loadWakaStatistics(context).projectStats[projectName]
    val statsData = mapOf<String, Int>(
        "Today" to (projectStats?.today ?: 0),
        "Last 7 Days" to (projectStats?.last7Days ?: 0),
        "Last 30 Days" to (projectStats?.last30Days ?: 0),
        "Past Year" to (projectStats?.lastYear ?: 0),
        "Total Time" to (projectStats?.allTime ?: 0),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        for ((key, value) in statsData) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = WakaHelpers.durationInSecondsToDurationString(value),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
