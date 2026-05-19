package com.aught.wakawaka

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aught.wakawaka.screens.home.HomeView
import com.aught.wakawaka.screens.projects.ProjectsView
import com.aught.wakawaka.screens.settings.SettingsView
import com.aught.wakawaka.ui.theme.WakaWakaTheme
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.aught.wakawaka.screens.projects.ProjectDetailsView
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.widget.WakaWidgetHelpers
import com.aught.wakawaka.widget.project.WakaProjectWidget
import com.aught.wakawaka.workers.TargetReminderWorker
import com.aught.wakawaka.workers.WakaDataFetchWorker
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit


sealed class Screen(val route: String, val name: String, val icon: ImageVector) {
    object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object ProjectDetails : Screen("projectDetails/{projectName}", "Project Details", Icons.Default.Folder) {
        fun createRoute(projectName: String) = "projectDetails/$projectName"
    }
}

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {

        // if does not have notification permission, request
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // make a work request to fetch data on first render
//        val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataFetchWorker>().build()
//        WorkManager.getInstance(this).enqueue(immediateWorkRequest)

        // Make a one-time work request to fetch data on first render. This is good practice.
        val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataFetchWorker>().build()
        WorkManager.getInstance(this).enqueue(immediateWorkRequest)

        // The periodic work should be enqueued here to ensure it is always scheduled
        // when the app is launched. Using `KEEP` prevents duplicate workers.
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            WakaDataFetchWorker::class.java,
            // Per WorkManager best practices, the minimum repeat interval is 15 minutes.
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WakaWakaDataFetch",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        // Schedule reminder workers for 6pm and 10pm
        scheduleReminderWorkers()

        // Schedule forced fetches at each completion-tier cutoff so the progress map has an entry
        // close to each boundary; the worker self-reschedules each day after firing.
        WakaDataFetchWorker.scheduleAllTierFetches(this)

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
    }

    private fun scheduleReminderWorkers() {
        val now = ZonedDateTime.now(ZoneId.systemDefault())

        // Schedule 6pm reminder
        val sixPm = now.withHour(WakaHelpers.REMINDER_6PM_HOUR).withMinute(0).withSecond(0)
        val sixPmDelay = if (now.isAfter(sixPm)) {
            // Schedule for tomorrow
            java.time.Duration.between(now, sixPm.plusDays(1)).toMillis()
        } else {
            // Schedule for today
            java.time.Duration.between(now, sixPm).toMillis()
        }

        val sixPmWorkRequest = OneTimeWorkRequestBuilder<TargetReminderWorker>()
            .setInitialDelay(sixPmDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TargetReminderWorker.REMINDER_TYPE_KEY to TargetReminderWorker.REMINDER_6PM))
            .addTag("reminder_6pm")
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "reminder_6pm",
            ExistingWorkPolicy.KEEP,
            sixPmWorkRequest
        )

        // Schedule 10pm reminder
        val tenPm = now.withHour(WakaHelpers.REMINDER_10PM_HOUR).withMinute(0).withSecond(0)
        val tenPmDelay = if (now.isAfter(tenPm)) {
            // Schedule for tomorrow
            java.time.Duration.between(now, tenPm.plusDays(1)).toMillis()
        } else {
            // Schedule for today
            java.time.Duration.between(now, tenPm).toMillis()
        }

        val tenPmWorkRequest = OneTimeWorkRequestBuilder<TargetReminderWorker>()
            .setInitialDelay(tenPmDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TargetReminderWorker.REMINDER_TYPE_KEY to TargetReminderWorker.REMINDER_10PM))
            .addTag("reminder_10pm")
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "reminder_10pm",
            ExistingWorkPolicy.KEEP,
            tenPmWorkRequest
        )

        val ogProjectId = intent.getStringExtra(WakaWidgetHelpers.WIDGET_INTENT_ID.toString())

        setContent {
            WakaWakaTheme {
                WakaWakaApp(ogProjectId)
            }
        }
    }
}


@Composable
fun WakaWakaApp(
    ogProjectId: String? = null
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            MyBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Projects.route) {
                ProjectsView(navController)
            }
            composable(Screen.Home.route) {
                HomeView(ogProjectId)
            }
            composable(Screen.Settings.route) {
                SettingsView()
            }
            composable(
                route = Screen.ProjectDetails.route,
                arguments = listOf(navArgument("projectName") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
                ProjectDetailsView(projectName, navController)

            }
        }
    }
}


@Composable
fun MyBottomNavigationBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Projects,
        Screen.Home,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        screens.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = screen.name,
                        modifier = Modifier.size(32.dp),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = Color.Transparent,
                ),
//                label = { Text(screen.name) },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
