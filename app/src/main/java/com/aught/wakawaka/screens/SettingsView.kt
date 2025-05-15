package com.aught.wakawaka.screens

import DailyTargetCard
import SuccessAlert
import WeeklyTargetCard
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.DataDump
import com.aught.wakawaka.data.DayOfWeek
import com.aught.wakawaka.data.StreakData
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaURL
import com.aught.wakawaka.utils.JSONDateAdapter
import com.aught.wakawaka.workers.WakaDataDumpWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

//    val aggregateData = WakaDataWorker.loadAggregateData(context)
    val aggregateData =
        WakaDataFetchWorker.loadAggregateData(context) ?: WakaHelpers.INITIAL_AGGREGATE_DATA


    // State variables for form fields
    var wakatimeAPIKey by remember {
        mutableStateOf(prefs.getString(WakaHelpers.WAKATIME_API, "") ?: "")
    }

    var wakapiAPIKey by remember {
        mutableStateOf(prefs.getString(WakaHelpers.WAKAPI_API, "") ?: "")
    }

    var withDailyTarget by remember {
        mutableStateOf(
            aggregateData.dailyTargetHours != null
        )
    }
    var dailyTarget by remember {
        mutableStateOf(aggregateData?.dailyTargetHours ?: 2f)
    }

    var withWeeklyTarget by remember {
        mutableStateOf(
            aggregateData?.weeklyTargetHours != null
        )
    }
    var weeklyTarget by remember {
        mutableStateOf(aggregateData?.weeklyTargetHours ?: 10f)
    }

//    val apiOptions = listOf(WakaURL.WAKATIME.url, WakaURL.WAKAPI.url)
//    var selectedApiOption by remember {
//        mutableStateOf(
//            prefs.getString(WakaHelpers.WAKA_URL, WakaURL.WAKATIME.url) ?: WakaHelpers.WAKA_URL
//        )
//    }
    val selectedApiOption = WakaURL.WAKATIME.url

    var selectedThemeOption by remember {
//        mutableStateOf(prefs.getInt(WakaHelpers.THEME, 0))
        // enforce dark mode
        mutableStateOf(1)
    }

    var dailyStreakExcludedDays by remember {
        mutableStateOf(
            aggregateData?.excludedDaysFromDailyStreak ?: listOf(DayOfWeek.SUNDAY.index)
        )
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
        )

        Spacer(modifier = Modifier.height(16.dp))

        val apiKey =
            if (selectedApiOption == WakaURL.WAKATIME.url) wakatimeAPIKey else wakapiAPIKey

        // API Key Input
        APIKeyCard(
            apiKey = apiKey,
            onApiKeyChange = {
                if (selectedApiOption == WakaURL.WAKATIME.url) {
                    wakatimeAPIKey = it
                } else {
                    wakapiAPIKey = it
                }
            },
            isWakatime = selectedApiOption == WakaURL.WAKATIME.url
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

        // Theme Selection Section
//        ThemeSelectionCard(
//            selectedThemeOption = selectedThemeOption,
//            onThemeOptionChange = { selectedThemeOption = it }
//        )

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
                delay(2000)
                showSuccessMessage = false
            }
        }

        // Success message
        SuccessAlert("Aggregate settings saved successfully!", showSuccessMessage)

        val crtScope = rememberCoroutineScope()


        // Save Button
        Button(
            onClick = {
                val updatedAggregateData = AggregateData(
                    aggregateData.dailyRecords,
                    if (withDailyTarget) dailyTarget else null,
                    if (withWeeklyTarget) weeklyTarget else null,
                    // if the daily target hours or weekly target hours have been changed, reset the streaks so that they can be recalculated
                    if (
                        aggregateData.dailyTargetHours != dailyTarget ||
                        // or if excluded days have been changed
                        aggregateData.excludedDaysFromDailyStreak != dailyStreakExcludedDays
                    ) StreakData(
                        0,
                        WakaHelpers.ZERO_DAY
                    ) else aggregateData.dailyStreak,
                    if (aggregateData.weeklyTargetHours != weeklyTarget) StreakData(
                        0,
                        WakaHelpers.ZERO_DAY
                    ) else aggregateData.weeklyStreak,
                    dailyStreakExcludedDays
                )

                WakaDataFetchWorker.saveAggregateData(context, updatedAggregateData)

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
                    val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataFetchWorker>().build()
                    WorkManager.getInstance(context).enqueue(immediateWorkRequest)

//                    WakaWidget().updateAll(context)

                    // Schedule the periodic
                    val workRequest = PeriodicWorkRequestBuilder<WakaDataFetchWorker>(
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


        DataDumpCard(context)
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
                style = MaterialTheme.typography.titleMedium
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataDumpCard(context: Context) {
    val moshi = Moshi.Builder()
        .add(JSONDateAdapter())
        .addLast(KotlinJsonAdapterFactory()).build()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            val jsonString = readTextFromUri(context, uri)
            if (jsonString == null) {
                println("Failed to read JSON from URI")
                return@let
            }
            println("first 100 chars of json read are ${jsonString.take(100)}")
            val dataDump = moshi.adapter(DataDump::class.java).fromJson(jsonString)
            if (dataDump != null) {
                WakaDataDumpWorker.saveDataDumpToLocalStorage(context, dataDump)
                println("Download successful: ${dataDump.user}")
            } else {
                println("Failed to parse DataDump")
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    launcher.launch("application/json")

//                crtScope.launch {
//                    // Schedule the one time immediate worker
//                    val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataDumpWorker>().build()
//                    WorkManager.getInstance(context).enqueue(immediateWorkRequest)
//                }
                }
            ) {
                Text(text = "Import Wakatime Data")
            }
        }
    }
}

fun readTextFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        }
    } catch (e: Exception) {
        null
    }
}
