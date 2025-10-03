package com.aught.wakawaka.screens.settings

import AlertData
import DailyTargetCard
import AlertPane
import WeeklyTargetCard
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.workers.WakaDataFetchWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.Duration

const val DEFAULT_DAILY_TARGET = 2f
const val DEFAULT_WEEKLY_TARGET = 10f


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var currSettings by remember {
        mutableStateOf(settings)
    }

    LaunchedEffect(settings) {
        // when settings changes, change currSettings
        currSettings = settings
    }

//    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

//    val aggregateData = WakaDataWorker.loadAggregateData(context)
//    val aggregateData =
//        WakaDataFetchWorker.loadAggregateData(context) ?: WakaHelpers.INITIAL_AGGREGATE_DATA


    // State variables for form fields
//    var wakatimeAPIKey by remember {
//        mutableStateOf(prefs.getString(WakaHelpers.WAKATIME_API, "") ?: "")
//    }

//    var withDailyTarget by remember {
//        mutableStateOf(
//            aggregateData.dailyTargetHours != null
//        )
//    }
//    var dailyTarget by remember {
//        mutableStateOf(aggregateData?.dailyTargetHours ?: 2f)
//    }

//    var withWeeklyTarget by remember {
//        mutableStateOf(
//            aggregateData?.weeklyTargetHours != null
//        )
//    }
//    var weeklyTarget by remember {
//        mutableStateOf(aggregateData?.weeklyTargetHours ?: 10f)
//    }
//
//    val selectedApiOption = WakaURL.WAKATIME.url
//
//
//    var dailyStreakExcludedDays by remember {
//        mutableStateOf(
//            aggregateData?.excludedDaysFromDailyStreak ?: listOf(DayOfWeek.SUNDAY.index)
//        )
//    }


    var alertData by remember { mutableStateOf<AlertData?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )


        // API Key Input
        APIKeyCard(
            apiKey = currSettings.wakatimeAPIKey,
            onApiKeyChange = {
//                wakatimeAPIKey = it
                currSettings = currSettings.copy(
                    wakatimeAPIKey = it
                )
            },
            isWakatime = true
        )


        Spacer(modifier = Modifier.height(8.dp))

        DailyTargetCard(
            dailyTarget = currSettings.dailyTargetHours ?: DEFAULT_DAILY_TARGET,
            withDailyTarget = currSettings.dailyTargetHours != null,
            dailyStreakExcludedDays = currSettings.dailyStreakExcludedDays,
            onDailyTargetChange = {
                currSettings = currSettings.copy(
                    dailyTargetHours = it
                )
            },
            onWithDailyTargetChange = {
                currSettings = currSettings.copy(
                    dailyTargetHours = if (it) DEFAULT_DAILY_TARGET else null
                )
            },
            onExcludedDaysChange = {
                currSettings = currSettings.copy(
                    dailyStreakExcludedDays = it
                )
            }
        )

        // Weekly Target Section

        WeeklyTargetCard(
            weeklyTarget = currSettings.weeklyTargetHours ?: DEFAULT_WEEKLY_TARGET,
            withWeeklyTarget = currSettings.weeklyTargetHours != null,
            onWeeklyTargetChange = {
                currSettings = currSettings.copy(
                    weeklyTargetHours = it
                )
            },
            onWithWeeklyTargetChange = {
                currSettings = currSettings.copy(
                    weeklyTargetHours = if (it) DEFAULT_WEEKLY_TARGET else null
                )
            }
        )


        Spacer(modifier = Modifier.height(16.dp))

        val apiKeyFilled = currSettings.wakatimeAPIKey.isNotEmpty()

        // Info card
        if (!apiKeyFilled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "API Key is required to fetch your WakaTime data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        LaunchedEffect(alertData) {
            if (alertData != null) {
                // Hide the success message after 2 seconds
                delay(2000)
                alertData = null
            }
        }

        // Success message
//        AlertPane(alertData, alertData != null)

        val crtScope = rememberCoroutineScope()


        // Save Button
        Button(
            onClick = {
//                // Save aggregate data to preferences
//                val updatedAggregateData = AggregateData(
//                    aggregateData.dailyRecords,
//                    if (withDailyTarget) dailyTarget else null,
//                    if (withWeeklyTarget) weeklyTarget else null,
//                    // if the daily target hours or weekly target hours have been changed, reset the streaks so that they can be recalculated
//                    if (
//                        aggregateData.dailyTargetHours != dailyTarget ||
//                        // or if excluded days have been changed
//                        aggregateData.excludedDaysFromDailyStreak != dailyStreakExcludedDays
//                    ) StreakData(
//                        0,
//                        WakaHelpers.ZERO_DAY
//                    ) else aggregateData.dailyStreak,
//                    if (aggregateData.weeklyTargetHours != weeklyTarget) StreakData(
//                        0,
//                        WakaHelpers.ZERO_DAY
//                    ) else aggregateData.weeklyStreak,
//                    dailyStreakExcludedDays
//                )
//
//                WakaDataFetchWorker.saveAggregateData(context, updatedAggregateData)
//
//                // Save settings to preferences
//                prefs.edit().apply {
//                    putString(WakaHelpers.WAKA_URL, selectedApiOption)
//                    putString(WakaHelpers.WAKATIME_API, wakatimeAPIKey)
//                    apply()
//                }
//
                viewModel.saveSettings(currSettings)
                crtScope.launch {
                    // Schedule the one time immediate worker
                    val immediateWorkRequest =
                        OneTimeWorkRequestBuilder<WakaDataFetchWorker>().build()
                    WorkManager.getInstance(context).enqueue(immediateWorkRequest)

                    // Schedule the periodic
                    val workRequest = PeriodicWorkRequestBuilder<WakaDataFetchWorker>(
                        // every hour
                        repeatInterval = Duration.ofMinutes(15),
                    ).build()

                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "WakaWakaDataFetch",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                    )
                }
                alertData = AlertData(
                    "Settings saved successfully!",
                    AlertType.Success
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKeyFilled,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Save Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.6f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary
        )

        DataSection(context) {
            alertData = it
        }


        // alert dialog instead of pane
        if (alertData != null) {
            AlertDialog(
                onDismissRequest = {
                    alertData = null
                },
                title = {
                    Text(
                        when (alertData?.type) {
                            AlertType.Success -> "Success"
                            AlertType.Failure -> "Error"
//                        AlertType.Warning -> "Warning"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center
                        ),
                    )
                },
                text = {
                    Text(alertData?.message ?: "")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            alertData = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIKeyCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isWakatime: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = apiKey,
        onValueChange = {
            onApiKeyChange(it)
        },
        label = { Text("${if (isWakatime) "WakaTime" else "Wakapi"} API Key") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = "API Key") },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionCard(
    selectedThemeOption: Int,
    onThemeOptionChange: (Int) -> Unit
) {
    val themeOptions = listOf("Light", "Dark", "System Default")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
            )

            themeOptions.forEachIndexed { index, theme ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedThemeOption == index,
                        onClick = {
                            onThemeOptionChange(index)
                        }
                    )
                    Text(
                        text = theme,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

