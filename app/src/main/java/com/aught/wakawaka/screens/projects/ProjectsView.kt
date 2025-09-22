package com.aught.wakawaka.screens.projects

import com.aught.wakawaka.screens.badges.HourCountBadge
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aught.wakawaka.data.WakaHelpers
import androidx.core.content.edit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.Screen
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.GraphMode
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.data.getPeriodicDates
import com.aught.wakawaka.utils.ColorUtils
import com.aught.wakawaka.widget.WakaWidgetHelpers
import com.aught.wakawaka.workers.WakaProjectWidgetUpdateWorker
import org.koin.androidx.compose.koinViewModel
import scrollBlurEffects
import kotlin.math.min
import kotlin.math.roundToInt


val GRAPH_HEIGHT = 200f


fun setProjectAssignedToProjectWidget(context: Context, projectName: String?) {
    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
    prefs.edit() {
        putString(WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET, projectName)
    }
    val immediateWorkRequest = OneTimeWorkRequestBuilder<WakaProjectWidgetUpdateWorker>().build()
    WorkManager.getInstance(context).enqueue(immediateWorkRequest)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsView(
    navController: NavHostController,
    viewModel: ProjectsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
    val wakaDataHandler = WakaDataHandler.fromContext(context)


    var projectAssignedToProjectWidget by remember {
        mutableStateOf(
            prefs.getString(
                WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET,
                null
            )
        )
    }

    var expandedProjectName by remember {
        mutableStateOf<String?>(null)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }
    var showSearchBar by remember {
        mutableStateOf(false)
    }

    val projects by viewModel.projects.collectAsState()

    val noProjectsAvailable = projects.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineMedium,
            )
            if (showSearchBar) {
                IconButton(
                    {
                        if (!noProjectsAvailable) {
                            showSearchBar = false
                            searchQuery = ""
                        }
                    },
                    enabled = !noProjectsAvailable,
                    modifier = Modifier
                        .padding(0.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Collapse Search",
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    {
                        if (!noProjectsAvailable) {
                            showSearchBar = true
                            searchQuery = ""
                        }
                    },
//                    enabled = wakaDataHandler.sortedProjectList.isNotEmpty(),
                    enabled = !noProjectsAvailable,
                    modifier = Modifier
                        .padding(0.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Expand Search",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            if (showSearchBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)

                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                        },
                        placeholder = {
                            Text(
                                text = "Search Projects...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f),
                                    fontWeight = FontWeight.Light,
                                    fontSize = 16.sp
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
//                            modifier = Modifier.padding(end = 4.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                    )

                }
            }
        }

//        )

        val lazyListState = rememberLazyListState()

        if (!noProjectsAvailable) {
            val projects = projects.filter {
                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .scrollBlurEffects(
                        lazyListState, projects.size, 0.1f
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                    state = lazyListState,
                ) {
                    items(projects) {
                        val projectName = it.name
                        val isProjectAssignedToProjectWidget =
                            projectAssignedToProjectWidget == projectName
                        val isExpanded = expandedProjectName == projectName

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .animateContentSize()
                        ) {
                            val totalHours: Int = it.dailyDurationInSeconds.values.sum() / 3600
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedProjectName = if (isExpanded) null else projectName
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
//                                Spacer(modifier = Modifier.padding(start = 16.dp))
                                    Text(
                                        text = WakaHelpers.truncateLabel(projectName, 28),
                                        modifier = Modifier
                                            .padding(16.dp)
                                    )
                                    if (isProjectAssignedToProjectWidget) {
                                        Icon(
                                            Icons.Default.Widgets,
                                            contentDescription = "Project assigned to Widget",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .fillMaxHeight()
                                ) {
                                    HourCountBadge(totalHours)

                                    Spacer(modifier = Modifier.width(3.dp))

                                    IconButton(
//                            modifier = Modifier.width(18.dp),
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(20.dp),
                                        onClick = {
                                            // navigate to project details
                                            navController.navigate(
                                                Screen.ProjectDetails.createRoute(
                                                    projectName
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit project settings",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                            if (isExpanded) {
                                ProjectGraph(projectName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isProjectAssignedToProjectWidget) "Deassign $projectName from Widget" else
                                            "Assign $projectName to Widget",
                                        fontSize = 12.sp
                                    )
                                    Switch(

                                        checked = isProjectAssignedToProjectWidget,
                                        onCheckedChange = {
                                            val newProjectAssignedToWidget =
                                                if (isProjectAssignedToProjectWidget) null else projectName
                                            projectAssignedToProjectWidget =
                                                newProjectAssignedToWidget
                                            setProjectAssignedToProjectWidget(
                                                context,
                                                newProjectAssignedToWidget
                                            )
                                        },
                                        modifier = Modifier.scale(0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No projects yet :(",
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )

            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectGraph(
    projectName: String,
    viewModel: ProjectsViewModel = koinViewModel()
) {
    var graphMode by remember {
        mutableStateOf(GraphMode.Daily)
    }
    val timePeriod = when (graphMode) {
        GraphMode.Daily -> TimePeriod.DAY
        GraphMode.Weekly -> TimePeriod.WEEK
    }

    // graph data
    val gd = viewModel.getProjectGraphData(projectName, timePeriod) ?: return

    val maxHours =
        when (graphMode) {
            GraphMode.Daily -> 24 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
            GraphMode.Weekly -> 24 * 7 * WakaWidgetHelpers.TIME_WINDOW_PROPORTION
        }

    val textColor = ColorUtils.desaturate(gd.color, 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((GRAPH_HEIGHT + 50 + WakaWidgetHelpers.GRAPH_BOTTOM_PADDING + WakaWidgetHelpers.DATE_TEXT_HEIGHT).dp)
            .padding(start = 12.dp, end = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .height(100.dp)
                .padding(start = 10.dp, top = 40.dp)
        ) {
            StreakDisplay(gd.streak, gd.hitTarget, textColor)
        }

        DurationScale(5, maxHours, textColor)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = WakaHelpers.truncateLabel(projectName).uppercase(),
                    color = gd.color,
                    fontSize = 16.sp,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DAILY",
                        fontSize = 12.sp,
                        color = when (graphMode) {
                            GraphMode.Daily -> gd.color
                            GraphMode.Weekly -> Color.Gray
                        },
                        fontWeight = when (graphMode) {
                            GraphMode.Daily -> FontWeight.Bold
                            GraphMode.Weekly -> FontWeight.Normal
                        },
                        modifier = Modifier
                            .clickable {
                                graphMode = GraphMode.Daily
                            }
                    )
                    Text(
                        text = "WEEKLY",
                        fontSize = 12.sp,
                        color = when (graphMode) {
                            GraphMode.Daily -> Color.Gray
                            GraphMode.Weekly -> gd.color
                        },
                        fontWeight = when (graphMode) {
                            GraphMode.Daily -> FontWeight.Normal
                            GraphMode.Weekly -> FontWeight.Bold
                        },
                        modifier = Modifier.clickable {
                            graphMode = GraphMode.Weekly
                        }
                    )

                }
            }
            // graph container
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .height((GRAPH_HEIGHT + WakaWidgetHelpers.GRAPH_BOTTOM_PADDING + WakaWidgetHelpers.DATE_TEXT_HEIGHT).dp)
                    .padding(start = 8.dp, end = 16.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .background(Color.Transparent)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(bottom = WakaWidgetHelpers.GRAPH_BOTTOM_PADDING.dp),
                ) {
                    // map all days or weeks depending on graph mode
                    gd.dates.zip(gd.data).forEachIndexed { i, it ->
                        val date = it.first
                        val duration = it.second

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(1 / (7f - i))
                                .padding(horizontal = 3.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            val barColor =
                                if (
                                    gd.targetInHours == null ||
                                    // if the day is in the exclusion list, use the primary color
                                    date.dayOfWeek.value in gd.excludedDays ||
                                    duration > (gd.targetInHours * 3600)
                                ) gd.color
                                else
                                    Color.Gray

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(
                                        (GRAPH_HEIGHT * min(
                                            1f,
                                            duration / (3600 * maxHours)
                                        )).dp
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(barColor)
                                    .padding(4.dp)
                            ) {}
                            // get the day,month and year from date of format yyyy-mm-dd
                            val date = WakaHelpers.dateToYYYYMMDD(date).split("-")
                            Box(
                                modifier = Modifier
                                    .height(WakaWidgetHelpers.DATE_TEXT_HEIGHT.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date[2] + "/" + date[1],
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    color = textColor
                                )
                            }

                        }
                    }
                }

            }
        }

        if (gd.targetInHours != null) {
            TargetLine(gd.targetInHours, maxHours)
        }
    }
}

@Composable
fun TargetLine(targetHours: Float, maxHours: Float) {
    // Target line - positioned at a specific height from bottom
    // For example, if your target is 4 hours (3600*4 seconds)
    val targetHeight = (GRAPH_HEIGHT * min(
        1f,
        (3600 * targetHours) / (3600 * maxHours)
    ) + WakaWidgetHelpers.GRAPH_BOTTOM_PADDING + WakaWidgetHelpers.DATE_TEXT_HEIGHT)

    val targetText =
        WakaHelpers.durationInSecondsToDurationString((targetHours * 3600).roundToInt())


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = targetHeight.dp, start = 10.dp, end = 20.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            Text(
                text = targetText,
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .background(Color.Black)
                    .padding(2.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val numberOfDashes = 20;

            (1..numberOfDashes).forEach {
                // Add the dotted line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1 / (numberOfDashes - it).toFloat())
                        .height(1.dp) // Line thickness
                        .padding(horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.White
                            )
                    ) {}
                }
            }
        }
    }
}


@Composable
fun DurationScale(numMarkers: Int, maxHours: Float, textColor: Color) {
    val interval = (maxHours / numMarkers).toInt()
    val intervalDp = (GRAPH_HEIGHT * min(
        1f, interval / maxHours
    ))
    val heightOfUnitMarker = 20

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = (WakaWidgetHelpers.DATE_TEXT_HEIGHT + WakaWidgetHelpers.GRAPH_BOTTOM_PADDING).dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
//        Text(
//            text = "h",
//            fontSize = 12.sp,
//            color = textColor,
//            textAlign = TextAlign.Start,
//            modifier = Modifier
//                .padding(bottom = 2.dp)
//                .width(20.dp)
//        )
        (1..numMarkers).reversed().forEach {

            Column(
                modifier = Modifier
                    .height(intervalDp.dp)
                    .width(25.dp)
            ) {
                Row(
                    modifier = Modifier
                        .height(heightOfUnitMarker.dp)
                        .fillMaxWidth()
                        .padding(end = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(2.dp)
                            .background(Color.White),
                    ) {}
                    Text(
                        text = "${it * interval}",
                        fontSize = 8.sp,
                        color = textColor
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .height((heightOfUnitMarker / 2).dp)
                .width(25.dp)
                .padding(start = 0.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(2.dp)
                    .background(Color.White),
            ) {}
        }
    }
}


@Composable
fun StreakDisplay(streak: Int, hitTargetToday: Boolean, textColor: Color) {
    val trueStreak = if (hitTargetToday) streak + 1 else streak
//    val streakColors = ColorUtils.getStreakColors(trueStreak)

    Text(
        text = trueStreak.toString(),
        color = textColor,
        fontSize = 56.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
    )
}


// region HOURS BADGE
// ? ........................


// ? ........................
// endregion ........................

