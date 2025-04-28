package com.aught.wakawaka

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.ui.theme.WakaWakaTheme
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakaWakaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsView(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsView(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(onClick = {
            val workRequest = OneTimeWorkRequestBuilder<WakaDataWorker>(
            ).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "WakaWakaDataFetch",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            val targetInHours = 5f;

            // save the target to prefs
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            prefs.edit() { putFloat(WakaHelpers.TARGET_HOURS, targetInHours) }

        }) {
            Text(text = "Click me to start")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
}
