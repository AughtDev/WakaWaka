package com.aught.wakawaka.screens

import DailyTargetCard
import WeeklyTargetCard
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.Screen
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.DayOfWeek
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.StreakData
import com.aught.wakawaka.data.WakaData
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaURL
import kotlinx.coroutines.launch
import java.time.Duration

@Composable
fun ProjectDetailsView(projectName: String, navController: NavHostController) {
    val context = LocalContext.current
    val projectSpecificData = WakaDataWorker.loadProjectSpecificData(context)
    val projectData = projectSpecificData[projectName]

    if (projectData != null) {

        var withDailyTarget by remember {
            mutableStateOf(
                projectData.dailyTargetHours != null
            )
        }
        var dailyTarget by remember {
            mutableStateOf(projectData.dailyTargetHours ?: 0f)
        }

        var withWeeklyTarget by remember {
            mutableStateOf(
                projectData.weeklyTargetHours != null
            )
        }
        var weeklyTarget by remember {
            mutableStateOf(projectData.weeklyTargetHours ?: 0f)
        }

        var dailyStreakExcludedDays by remember {
            mutableStateOf(
                projectData.excludedDaysFromDailyStreak
            )
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

                // Save Button
                Button(
                    onClick = {
                        WakaDataWorker.saveProjectData(
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
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save Project Settings")
                }
            }
        }
    } else {
        // Handle the case where project data is not found
        Text(text = "Project data not found")
    }
}
