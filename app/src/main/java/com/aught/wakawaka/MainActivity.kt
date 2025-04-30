package com.aught.wakawaka

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.ui.theme.WakaWakaTheme
import com.aught.wakawaka.widget.WakaWidget
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Duration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakaWakaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsView(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

    // State variables for form fields
    var apiKey by remember {
        mutableStateOf(prefs.getString(WakaHelpers.WAKATIME_API, "") ?: "")
    }
    var passwordVisible by remember { mutableStateOf(false) }

    var dailyTarget by remember {
        mutableStateOf(prefs.getFloat(WakaHelpers.DAILY_TARGET_HOURS, 5f))
    }
    var weeklyTarget by remember {
        mutableStateOf(prefs.getFloat(WakaHelpers.WEEKLY_TARGET_HOURS, 40f))
    }

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
            text = "Widget Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // API Key Input
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("WakaTime API Key") },
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
                    steps = 35,
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

        // Info card
        if (apiKey.isEmpty()) {
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

        // Save Button
        Button(
            onClick = {
                val ogDailyTarget = prefs.getFloat(WakaHelpers.DAILY_TARGET_HOURS, 5f)
                val ogWeeklyTarget = prefs.getFloat(WakaHelpers.WEEKLY_TARGET_HOURS, 40f)

                // if there has been a new target, reset the streaks
                if (ogDailyTarget != dailyTarget) {
                    WakaDataWorker.resetStreak(context, GraphMode.Daily)
                }
                if (ogWeeklyTarget != weeklyTarget) {
                    WakaDataWorker.resetStreak(context, GraphMode.Weekly)
                }

                // Save settings to preferences
                prefs.edit().apply {
                    putString(WakaHelpers.WAKATIME_API, apiKey)
                    putFloat(WakaHelpers.DAILY_TARGET_HOURS, dailyTarget)
                    putFloat(WakaHelpers.WEEKLY_TARGET_HOURS, weeklyTarget)
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
                        repeatInterval = Duration.ofHours(1)
                    ).build()

                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "WakaWakaDataFetch",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest
                    )
                }


                // Show success message
                showSuccessMessage = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotEmpty()
        ) {
            Text("Save Settings")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
