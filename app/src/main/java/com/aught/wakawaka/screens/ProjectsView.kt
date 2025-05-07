package com.aught.wakawaka.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aught.wakawaka.data.WakaDataWorker
import com.aught.wakawaka.data.WakaHelpers
import androidx.core.content.edit

fun setProjectAssignedToProjectWidget(context: Context, projectName: String) {
    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
    prefs.edit() {
        putString(WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET, projectName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsView() {
    val context = LocalContext.current
    val projectSpecificData = WakaDataWorker.loadProjectSpecificData(context)
    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Projects",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        projectSpecificData.forEach { it ->
            val isProjectAssignedToProjectWidget = prefs.getString(
                WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET,
                null
            ) == it.key

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = it.key,
                        modifier = Modifier.padding(16.dp),
                    )

                    Row() {
                        IconButton(
                            onClick = {
                                setProjectAssignedToProjectWidget( context, it.key )
                            }
                        ) {
                            Icon(
                                Icons.Default.Widgets,
                                contentDescription = "Project assigned to Widget",
                                tint = if (isProjectAssignedToProjectWidget) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
