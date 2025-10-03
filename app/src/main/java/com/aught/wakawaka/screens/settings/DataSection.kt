package com.aught.wakawaka.screens.settings

import AlertData
import AlertPane
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aught.wakawaka.data.DataDump
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.utils.JSONDateAdapter
import com.aught.wakawaka.workers.WakaDataDumpWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import androidx.core.content.edit
import com.aught.wakawaka.utils.ColorUtils
import com.aught.wakawaka.utils.getMoshi
import okio.buffer
import okio.source


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

fun readAndParseDataDump(context: Context, uri: Uri, moshi: Moshi): DataDump? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val source = inputStream.source().buffer()
            moshi.adapter(DataDump::class.java).fromJson(source)
        }
    } catch (e: Exception) {
        Log.e("DataDump", "Failed to parse JSON", e)
        null
    }
}

// endregion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSection(context: Context,setAlertData: (AlertData?) -> Unit) {
    val moshi = getMoshi()


    // Header
    Text(
        text = "Data",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth()
    )

    // Success message
    JSONDataCard(context, moshi,setAlertData)

    BackupAndRestoreDataCard(context,setAlertData)

    DeleteDataCard(context,setAlertData)
}

@Composable
fun JSONDataCard(context: Context, moshi: Moshi,setAlertData: (AlertData?) -> Unit) {
//    var alertData by remember { mutableStateOf<AlertData?>(null) }

//
//    LaunchedEffect(alertData) {
//        if (alertData != null) {
//            // Hide the success message after 2 seconds
//            delay(2000)
//            alertData = null
//        }
//    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            Log.d("DataDump", "Selected URI: $uri, reading text")
            val dataDump = readAndParseDataDump(context, uri, moshi)
            if (dataDump != null) {
                WakaDataDumpWorker.saveDataDumpToLocalStorage(context, dataDump)
                println("Download successful: ${dataDump.user}")
                setAlertData(AlertData("Data imported successfully"))
            } else {
                println("Failed to parse DataDump")
                setAlertData(AlertData("Failed to parse json data", AlertType.Failure))
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
//    AlertPane(alertData, alertData != null)

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
                        style = MaterialTheme.typography.titleSmall
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
fun BackupAndRestoreDataCard(context: Context,setAlertData: (AlertData?) -> Unit) {
//    var alertData by remember { mutableStateOf<AlertData?>(null) }


//    LaunchedEffect(alertData) {
//        if (alertData != null) {
//            // Hide the success message after 2 seconds
//            delay(2000)
//            alertData = null
//        }
//    }
//
//    AlertPane(alertData, alertData != null)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = "Backup and Restore")
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            val coroutineScope = rememberCoroutineScope()
            Button(
                onClick = {
                    coroutineScope.launch {
                        val backupFile = BackupManager.createBackup(context)
                        if (backupFile != null) {
                            BackupManager.shareBackup(context, backupFile)
                            setAlertData(AlertData("Backup created and ready to share"))
                        } else {
                            setAlertData(AlertData("Backup failed", AlertType.Failure))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create Backup")
            }
            val moshi = getMoshi()
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
                    val backupData = moshi.adapter(BackupData::class.java).fromJson(jsonString)
                    if (backupData != null) {
                        BackupManager.restoreBackup(context, backupData)
                        println("Backup restoration successful")
                        setAlertData(AlertData("Backup restored successfully"))
                    } else {
                        println("Failed to parse backup data")
                        setAlertData(AlertData("Failed to parse backup data", AlertType.Failure))
                    }
                }
            }
            Button(
                onClick = {
                    launcher.launch("application/json")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Restore")
            }
        }
    }
}

@Composable
fun DeleteDataCard(context: Context,setAlertData: (AlertData?) -> Unit) {

//    var alertData by remember { mutableStateOf<AlertData?>(null) }
    var confirmationDialogOpen by remember { mutableStateOf(false) }


//    LaunchedEffect(alertData) {
//        if (alertData != null) {
//            // Hide the success message after 2 seconds
//            delay(2000)
//            alertData = null
//        }
//    }

//    AlertPane(alertData, alertData != null)

    if (confirmationDialogOpen) {
        Dialog(
            onDismissRequest = { confirmationDialogOpen = false }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal =16.dp,vertical = 24.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete all your data?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Text(
                    text = "Consider making a backup first. \n This action cannot be undone.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Button(
                    onClick = {
                        // wipe shared prefs
                        val prefs =
                            context.getSharedPreferences(
                                WakaHelpers.Companion.PREFS,
                                Context.MODE_PRIVATE
                            )

                        prefs.edit {
                            clear()
                        }

                        setAlertData(AlertData("Successfully wiped all app data"))
                        confirmationDialogOpen = false
                    },
//                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors().copy(
                        containerColor = Color.Red
                    )
                ) {
                    Text(text = "Yes", color = Color.White)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Wipe Data")
                Text(
                    text = "Wipe Data",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = "Warning, this will permanently delete all your data. Please make a backup before proceeding.",
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
                        confirmationDialogOpen = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(0.7f),
                        contentColor = Color.Red.copy(0.7f),
                        disabledContentColor = Color.Red.copy(0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Wipe App Data", color = Color.White)
                }
            }
        }
    }
}



