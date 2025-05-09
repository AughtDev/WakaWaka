package com.aught.wakawaka.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aught.wakawaka.Screen
import com.aught.wakawaka.data.WakaDataWorker

@Composable
fun ProjectDetailsView(projectName: String, navController: NavHostController) {
    val context = LocalContext.current
    val projectSpecificData = WakaDataWorker.loadProjectSpecificData(context)
    val projectData = projectSpecificData[projectName]
    if (projectData != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().height(64.dp)
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
        }
    } else {
        // Handle the case where project data is not found
        Text(text = "Project data not found")
    }
}
