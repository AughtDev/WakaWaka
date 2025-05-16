package com.aught.wakawaka.screens.settings

import SuccessAlert
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.DataDump
import com.aught.wakawaka.data.StreakData
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaURL
import com.aught.wakawaka.utils.JSONDateAdapter
import com.aught.wakawaka.workers.WakaDataDumpWorker
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration


// region HELPER FUNCTIONS

fun readTextFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        }
    } catch (e: Exception) {
        null
    }
}

// endregion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSection(context: Context) {
    val moshi = Moshi.Builder()
        .add(JSONDateAdapter())
        .addLast(KotlinJsonAdapterFactory()).build()


    // Header
    Text(
        text = "Data",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth()
    )

    // Success message
    JSONDataCard(context, moshi)

    BackupAndRestoreDataCard()
}

@Composable
fun JSONDataCard(context: Context, moshi: Moshi) {

    var showSuccessMessage by remember { mutableStateOf(false) }


    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            // Hide the success message after 2 seconds
            delay(2000)
            showSuccessMessage = false
        }
    }

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
                showSuccessMessage = true
            } else {
                println("Failed to parse DataDump")
            }
        }
    }

    var showInfoDialog by remember { mutableStateOf(false) }

    // Dialog for info
    if (showInfoDialog) {
        Dialog(
            onDismissRequest = { showInfoDialog = false }
        ) {
            Column {
                Text(
                    text = "To get the JSON data dump, go to the wakatime website at /settings/account and scroll down to 'Export my code stats...' ",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    SuccessAlert("Wakatime JSON Data imported successfully", showSuccessMessage)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(

                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Data Import")
                    Text(
                        text = "Data Import",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(
                    onClick = {
                        showInfoDialog = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Warning, this will overwrite your current data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        launcher.launch("application/json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Import Wakatime datadump JSON")
                }
            }
        }
    }
}


@Composable
fun BackupAndRestoreDataCard() {
    var successMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            // Hide the success message after 2 seconds
            delay(2000)
            successMessage = null
        }
    }

    SuccessAlert(successMessage ?: "-", successMessage != null)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = "Backup and Restore")
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(
                onClick = {
                    successMessage = "Created backup"

                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create Backup")
            }
            Button(
                onClick = {

                    successMessage = "Restored backup"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Restore")
            }
        }
    }
}




