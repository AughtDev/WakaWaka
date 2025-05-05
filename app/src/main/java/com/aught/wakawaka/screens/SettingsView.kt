package com.aught.wakawaka.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.StreakData
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaURL
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

    val aggregateData = WakaDataWorker.loadAggregateData(context)


    // State variables for form fields
    var wakatimeAPIKey by remember {
        mutableStateOf(prefs.getString(WakaHelpers.WAKATIME_API, "") ?: "")
    }

    var wakapiAPIKey by remember {
        mutableStateOf(prefs.getString(WakaHelpers.WAKAPI_API, "") ?: "")
    }

    var passwordVisible by remember { mutableStateOf(false) }

    var dailyTarget by remember {
        mutableStateOf(aggregateData?.dailyTargetHours ?: 0f)
    }
    var weeklyTarget by remember {
        mutableStateOf(aggregateData?.weeklyTargetHours ?: 0f)
    }

//    val apiOptions = listOf(WakaURL.WAKATIME.url, WakaURL.WAKAPI.url)
//    var selectedApiOption by remember {
//        mutableStateOf(
//            prefs.getString(WakaHelpers.WAKA_URL, WakaURL.WAKATIME.url) ?: WakaHelpers.WAKA_URL
//        )
//    }
    val selectedApiOption = WakaURL.WAKATIME.url

    val themeOptions = listOf("Light", "Dark", "System Default")
    var selectedThemeOption by remember {
        mutableStateOf(prefs.getInt(WakaHelpers.THEME, 0))
    }

    var showSuccessMessage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val apiKey =
            if (selectedApiOption == WakaURL.WAKATIME.url) wakatimeAPIKey else wakapiAPIKey

        // API Key Input
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                if (selectedApiOption == WakaURL.WAKATIME.url) {
                    wakatimeAPIKey = it
                } else {
                    wakapiAPIKey = it
                }
            },
            label = { Text("${if (selectedApiOption == WakaURL.WAKATIME.url) "WakaTime" else "Wakapi"} API Key") },
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

        // Api URL Selection
//        Card(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Icon(Icons.Default.Link, contentDescription = "Waka API")
//                    Text(
//                        text = "Waka API",
//                        style = MaterialTheme.typography.titleMedium
//                    )
//                }
//
//                apiOptions.forEachIndexed { index, api ->
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        RadioButton(
//                            selected = selectedApiOption == api,
//                            onClick = { selectedApiOption = api }
//                        )
//                        Text(
//                            text = if (api == WakaURL.WAKATIME.url) "Wakatime" else "Wakapi",
//                        )
//                    }
//                }
//
//
//            }
//
//        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Target Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Daily Target")
                    Text(
                        text = "Daily Target",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = "Set your daily coding goal in hours: ${
                        String.format(
                            "%.1f",
                            dailyTarget
                        )
                    } hours",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = dailyTarget,
                    onValueChange = { dailyTarget = it },
                    valueRange = 1f..12f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Weekly Target Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Weekly Target")
                    Text(
                        text = "Weekly Target",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = "Set your weekly coding goal in hours: ${
                        String.format(
                            "%.1f",
                            weeklyTarget
                        )
                    } hours",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = weeklyTarget,
                    onValueChange = { weeklyTarget = it },
                    valueRange = 5f..75f,
                    steps = 34,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }


        // Theme Selection Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium
                )

                themeOptions.forEachIndexed { index, theme ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedThemeOption == index,
                            onClick = { selectedThemeOption = index }
                        )
                        Text(
                            text = theme,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val apiKeyFilled = if (selectedApiOption == WakaURL.WAKATIME.url) {
            wakatimeAPIKey.isNotEmpty()
        } else {
            wakapiAPIKey.isNotEmpty()
        }

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

        LaunchedEffect(showSuccessMessage) {
            if (showSuccessMessage) {
                // Hide the success message after 2 seconds
                kotlinx.coroutines.delay(2000)
                showSuccessMessage = false
            }
        }

        // Success message
        AnimatedVisibility(visible = showSuccessMessage) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Settings saved successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
        val crtScope = rememberCoroutineScope()

        val aggregateData =
            WakaDataWorker.loadAggregateData(context) ?: WakaHelpers.INITIAL_AGGREGATE_DATA

        // Save Button
        Button(
            onClick = {
                val updatedAggregateData = AggregateData(
                    aggregateData.dailyRecords,
                    dailyTarget,
                    weeklyTarget,
                    // if the daily target hours or weekly target hours have been changed, reset the streaks so that they can be recalculated
                    if (aggregateData.dailyTargetHours != dailyTarget) StreakData(
                        0,
                        WakaHelpers.ZERO_DAY
                    ) else aggregateData.dailyStreak,
                    if (aggregateData.weeklyTargetHours != weeklyTarget) StreakData(
                        0,
                        WakaHelpers.ZERO_DAY
                    ) else aggregateData.weeklyStreak,
                    aggregateData.excludedDaysFromDailyStreak
                )

                WakaDataWorker.saveAggregateData(context, updatedAggregateData)

                // Save settings to preferences
                prefs.edit().apply {
                    putString(WakaHelpers.WAKA_URL, selectedApiOption)
                    if (selectedApiOption == WakaURL.WAKATIME.url) {
                        putString(WakaHelpers.WAKATIME_API, wakatimeAPIKey)
                    } else {
                        putString(WakaHelpers.WAKAPI_API, wakapiAPIKey)
                    }
                    putInt(WakaHelpers.THEME, selectedThemeOption)
                    apply()
                }

                crtScope.launch {
                    // Schedule the one time immediate worker
                    val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataWorker>().build()
                    WorkManager.getInstance(context).enqueue(immediateWorkRequest)

//                    WakaWidget().updateAll(context)

                    // Schedule the periodic
                    val workRequest = PeriodicWorkRequestBuilder<WakaDataWorker>(
                        // every hour
                        repeatInterval = Duration.ofHours(1),
                    ).build()

                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "WakaWakaDataFetch",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                    )
                }

                // Show success message
                showSuccessMessage = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKeyFilled
        ) {
            Text("Save Settings")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
