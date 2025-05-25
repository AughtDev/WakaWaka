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
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.screens.projects.ProjectDetailsView
import androidx.core.content.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.workers.WakaDataFetchWorker


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
        val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaDataFetchWorker>().build()
        WorkManager.getInstance(this).enqueue(immediateWorkRequest)


        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            WakaWakaTheme {
                WakaWakaApp()
            }
        }
    }
}


@Composable
fun WakaWakaApp() {
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
                HomeView()
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
