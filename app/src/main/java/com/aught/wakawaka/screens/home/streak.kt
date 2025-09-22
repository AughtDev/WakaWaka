package com.aught.wakawaka.screens.home

import com.aught.wakawaka.screens.badges.HourCountBadge
import com.aught.wakawaka.screens.badges.MILESTONES
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.TargetStreakData
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataTransformers
import com.aught.wakawaka.data.WakaDataUseCase
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.screens.badges.getMilestoneIndex
import org.koin.androidx.compose.koinViewModel
import scrollBlurEffects
import kotlin.math.max
import kotlin.math.roundToInt

// region DISPLAY
// ? ........................
// ?
sealed class ActiveStreakDialog {
    object None : ActiveStreakDialog()
    object Aggregate : ActiveStreakDialog()
    data class Project(val projectName: String) : ActiveStreakDialog()
}

@Composable
fun DailyStreakDisplay(
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
//    val selectedProject by viewModel.selectedProjectName.collectAsState()

//    val isAggregate by remember {
//        derivedStateOf {
//            selectedProject == WakaHelpers.ALL_PROJECTS_ID
//        }
//    }

    var activeDialog by remember {
        mutableStateOf<ActiveStreakDialog>(ActiveStreakDialog.None)
    }

//    val dailyTargetStreakData by viewModel.dailyTargetStreakData.collectAsState()

    Text(
        text = uiState.dailyTargetStreakData.streak.toString(),
        fontSize = 72.sp,
        color = if (uiState.dailyTargetStreakData.targetHit) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(0.5f)
        },
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                activeDialog = if (uiState.selectedProjectName == WakaHelpers.ALL_PROJECTS_ID) {
                    ActiveStreakDialog.Aggregate
                } else {
                    ActiveStreakDialog.Project(uiState.selectedProjectName)
                }
            }
            .padding(horizontal = 8.dp)
    )
    when (activeDialog) {
        is ActiveStreakDialog.None -> {
            // do nothing
        }

        is ActiveStreakDialog.Aggregate -> {
            Dialog(
                onDismissRequest = {
                    activeDialog = ActiveStreakDialog.None
                }
            ) {
                AggregateStreakDialog({
                    activeDialog = ActiveStreakDialog.Project(it)
                })
            }
        }

        is ActiveStreakDialog.Project -> {
            val projectName = (activeDialog as ActiveStreakDialog.Project).projectName
            Dialog(
                onDismissRequest = {
                    activeDialog = ActiveStreakDialog.None
                }
            ) {
                ProjectStreakDialog(
                    projectName, if (uiState.selectedProjectName != WakaHelpers.ALL_PROJECTS_ID) null else {
                    { activeDialog = ActiveStreakDialog.Aggregate }
                })
            }
        }
    }
}

// ? ........................
// endregion ........................

// region HELPERS
// ? ........................

fun getProjectColor(project: ProjectSpecificData): Color {
    return runCatching { Color(project.color.toColorInt()) }.getOrNull()
        ?: WakaHelpers.projectNameToColor(project.name)
}

fun getProjectTargetStreak(project: ProjectSpecificData, period: TimePeriod): TargetStreakData {
    // get duration today
    val target = when (period) {
        TimePeriod.DAY -> project.dailyTargetHours
        TimePeriod.WEEK -> project.weeklyTargetHours
        else -> null
    }

    val streak = when (period) {
        TimePeriod.DAY -> project.dailyStreak?.count ?: 0
        TimePeriod.WEEK -> project.weeklyStreak?.count ?: 0
        else -> 0
    }

    val duration = WakaDataTransformers.calcOffsetPeriodicDurationInSeconds(project.dailyDurationInSeconds, TimePeriod.DAY, 0)
    val completion = if (target == null) {
        if (duration > 0) 1f else 0f
    } else (duration.toFloat() / (target * 3600)).coerceIn(0f, 1f)

    return TargetStreakData(
        target = target,
        streak = streak + if (completion >= 1f) 1 else 0,
        completion = completion,
        targetHit = completion >= 1f
    )
}

// the target should be in the format 1 hr or 2 hrs or 3 hrs 30 mins
fun targetToText(target: Float?): String {
    if (target == null) return "?? hrs"
    val hours = target.toInt()
    val minutes = ((target - hours) * 60).roundToInt()
    return when {
        hours > 0 && minutes > 0 -> "$hours hrs $minutes mins"
        hours > 0 -> "$hours hrs"
        minutes > 0 -> "$minutes mins"
        else -> "0 hrs"
    }
}


// ? ........................
// endregion ........................


// region UI COMPONENTS
// ? ........................

@Composable
fun StreakValueDisplay(
    streak: Int,
    completion: Float,
    color: Color,
    textSize: Int = 72,
    cornerRadius: Int = textSize,
    xPadding: Int = 18,
    yPadding: Int = 8
) {
    val progressPathPadding = 16f

    val path = remember {
        Path()
    }

    val pathMeasure = remember {
        PathMeasure()
    }

    var segmentPath by remember {
        mutableStateOf(Path())
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(min = (textSize * 1.5f).dp)
//            .padding(8.dp)
            // draw the progress indicator around the text with a border radius
            .drawBehind {
                if (path.isEmpty) {
                    path.apply {
                        // Draw a rounded rectangle for the background
                        addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    progressPathPadding,
                                    progressPathPadding,
                                    size.width - progressPathPadding,
                                    size.height - progressPathPadding
                                ),
                                cornerRadius = CornerRadius(
                                    cornerRadius.toFloat(),
                                    cornerRadius.toFloat()
                                )
                            )
                        )
                    }
                    pathMeasure.setPath(path, true)
                }
                segmentPath.reset()

                val start = 0.8f
                pathMeasure.getSegment(
//                                0f,targetCompletion * pathMeasure.length,
                    pathMeasure.length * max(0f, start - completion),
                    pathMeasure.length * start,
                    segmentPath,
                    true
                )
                if (completion > start) {
                    pathMeasure.getSegment(
                        pathMeasure.length * (1f - (completion - start)),
                        pathMeasure.length * 1f,
                        segmentPath,
                        true
                    )
                }
                drawPath(
                    segmentPath,
                    color = color,
                    style = Stroke(
                        width = textSize / 4.5f,
                        pathEffect = null,
                        cap = StrokeCap.Round
                    )
                )
                drawPath(
                    path,
                    color = color.copy(0.2f),
                    style = Stroke(
                        width = textSize / 4.5f,
                        pathEffect = null,
                        cap = StrokeCap.Round
                    )
                )
            }
    ) {
        Text(
            text = streak.toString(),
            fontSize = textSize.sp,
            color = if (completion >= 1f) {
                color
            } else {
                MaterialTheme.colorScheme.onSurface.copy(0.6f)
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
//                .background(MaterialTheme.colorScheme.surfaceVariant)
//                .background(Color.Red)
                .padding(horizontal = xPadding.dp, vertical = yPadding.dp)
        )
    }

}

@Composable
fun StreakStatsDisplay(
    label: String,
    streak: Int,
    target: Float?,
    completion: Float,
    color: Color,
    cheatLabel: String,
    numCheatPeriods: Int = 0,
    cheatPeriodCompletion: Float = 0f,
    useCheatDay: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = targetToText(target),
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.tertiary.copy(0.5f),
                lineHeight = 8.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .height(10.dp)
                    .offset(y = (-4).dp)
            )
            Log.d(
                "StreakDisplay",
                "Rendering StreakValueDisplay for $label with streak: $streak, target: $target, completion: $completion, color: $color"
            )
            StreakValueDisplay(streak, completion, color)
        }

//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
//            Text(
//                text = cheatLabel.uppercase(),
//                fontSize = 10.sp,
//                color = MaterialTheme.colorScheme.primary.copy(0.5f),
//                fontWeight = FontWeight.SemiBold,
//            )
////            Row(
////                verticalAlignment = Alignment.CenterVertically,
////                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
////            ) {
//            StreakValueDisplay(
//                streak = numCheatPeriods,
//                completion = cheatPeriodCompletion,
//                color = MaterialTheme.colorScheme.tertiary,
//                textSize = 20,
//                xPadding = 12,
//                yPadding = 6
//            )
//
//            Text("USE")
////            }
//        }
    }

}

@Composable
fun HourMilestoneIndicator(
    color: Color,
    totalHours: Int,
) {
    val textMeasurer = rememberTextMeasurer()

    val nextMilestone = runCatching {
        MILESTONES[getMilestoneIndex(totalHours) + 1]
    }.getOrNull()

    val progress = if (nextMilestone == null) 1f else {
        totalHours.toFloat() / nextMilestone.hours
    }

//    Log.d("StreakDisplay", "Total Hours: $totalHours, Next Milestone: ${nextMilestone?.hours}, Color: $color, Progress: $progress")


    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp)
    ) {
        val width = size.width
        val height = size.height

        // Draw the background line
        drawLine(
            color = color.copy(0.3f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 32f,
            cap = StrokeCap.Round
        )

        // Draw the progress line
        drawLine(
            color = color,
            start = Offset(0f, height / 2),
            end = Offset(width * progress, height / 2),
            strokeWidth = 32f,
            cap = StrokeCap.Round
        )

        // Draw the milestone indicator
        val milestoneX = width * progress
        drawCircle(
            color = color,
            radius = 12f,
            center = Offset(milestoneX, height / 2)
        )
        drawText(
            textMeasurer,
            text = "${nextMilestone?.hours ?: 1000} hrs",
            topLeft = Offset(
                x = width - 130f,
                y = (height / 2) + 30f
            ),
            style = TextStyle(
                fontSize = 10.sp,
                color = color.copy(0.6f),
                textAlign = TextAlign.End
            ),
            size = Size(150f, 30f)
        )
    }
}

// ? ........................
// endregion ........................


// region DIALOGS
// ? ........................

@Composable
fun AggregateStreakDialog(
    goToProjectStreakDialog: (projectName: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val projects by viewModel.projects.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .clip(
                MaterialTheme.shapes.large
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
//                .background(Color.Red.copy(0.2f))
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
//                val dailyStreak =
//                    wakaDataHandler.getStreak(DataRequest.Aggregate, TimePeriod.DAY).count
//                val dailyTargetHit =
//                    wakaDataHandler.targetHit(DataRequest.Aggregate, TimePeriod.DAY)
                // daily streak
                StreakStatsDisplay(
                    label = "Daily",
                    streak = uiState.dailyTargetStreakData.streak,
                    target = uiState.dailyTargetStreakData.target,
                    completion = uiState.dailyTargetStreakData.completion,
//                    streak = dailyStreak + if (dailyTargetHit) 1 else 0,
//                    target = wakaDataHandler.aggregateData?.dailyTargetHours?.roundToInt(),
//                    completion = wakaDataHandler.getStreakCompletion(
//                        DataRequest.Aggregate,
//                        TimePeriod.DAY
//                    ),
                    color = MaterialTheme.colorScheme.primary,
                    cheatLabel = "Cheat Days",
                    numCheatPeriods = 4,
                    cheatPeriodCompletion = 0.5f,
                ) {
                    // todo: work on cheat day logic
                }

//                val weeklyStreak =
//                    wakaDataHandler.getStreak(DataRequest.Aggregate, TimePeriod.WEEK).count
//                val weeklyTargetHit =
//                    wakaDataHandler.targetHit(DataRequest.Aggregate, TimePeriod.WEEK)
                // weekly streak
                StreakStatsDisplay(
                    label = "Weekly",
                    streak = uiState.weeklyTargetStreakData.streak,
                    target = uiState.weeklyTargetStreakData.target,
                    completion = uiState.weeklyTargetStreakData.completion,
//                    streak = weeklyStreak + if (weeklyTargetHit) 1 else 0,
//                    target = wakaDataHandler.aggregateData?.weeklyTargetHours?.roundToInt(),
//                    completion = wakaDataHandler.getStreakCompletion(
//                        DataRequest.Aggregate,
//                        TimePeriod.WEEK
//                    ),
                    color = MaterialTheme.colorScheme.primary,
                    cheatLabel = "Cheat Weeks",
                    numCheatPeriods = 2,
                    cheatPeriodCompletion = 0.25f,
                ) {
                    // todo: work on cheat week logic
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.3f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROJECTS",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary.copy(0.3f),
                    lineHeight = 8.sp,
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth(0.5f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "DAILY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(0.3f),
                        lineHeight = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(10.dp)
                            .offset(x = 8.dp)
                    )
                    Text(
                        text = "WEEKLY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(0.3f),
                        lineHeight = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(10.dp)
                            .offset(x = (-4).dp)
                    )
                }
            }
            val lazyListState = rememberLazyListState()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .scrollBlurEffects(
                        lazyListState, projects.size
                    )
            ) {

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(projects) {
                        val projectColor = getProjectColor(it)

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .clickable {
                                    goToProjectStreakDialog(it.name)
                                },
                        ) {
                            Text(
                                text = WakaHelpers.truncateLabel(it.name, 30).uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = projectColor,
                                modifier = Modifier.fillMaxWidth(0.5f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
//                                    val dailyStreak = wakaDataHandler.getStreak(
//                                        DataRequest.ProjectSpecific(it.name),
//                                        TimePeriod.DAY
//                                    ).count
//                                    val dailyTargetHit = wakaDataHandler.targetHit(
//                                        DataRequest.ProjectSpecific(it.name),
//                                        TimePeriod.DAY
//                                    )
                                    val dailyStreakData = getProjectTargetStreak(it, TimePeriod.DAY)
                                    StreakValueDisplay(
                                        streak = dailyStreakData.streak,
                                        completion = dailyStreakData.completion,
                                        color = projectColor,
                                        textSize = 28,
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
//                                        )
//                                    val weeklyStreak = wakaDataHandler.getStreak(
//                                        DataRequest.ProjectSpecific(it.name),
//                                        TimePeriod.WEEK
//                                    ).count
//                                    val weeklyTargetHit = wakaDataHandler.targetHit(
//                                        DataRequest.ProjectSpecific(it.name),
//                                        TimePeriod.WEEK
//                                    )
                                    val weeklyStreakData = getProjectTargetStreak(it, TimePeriod.WEEK)
                                    StreakValueDisplay(
                                        streak = weeklyStreakData.streak,
                                        completion = weeklyStreakData.completion,
                                        color = projectColor,
                                        textSize = 28,
                                    )
                                }
                            }
                        }
                    }
                }
//                ScrollBlurEffects(lazyListState, projects.size, MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.7f))
            }

        }
    }
}


@Composable
fun ProjectStreakDialog(
    projectName: String,
    backToAggregateStreakDialog: (() -> Unit)?,
    viewModel: HomeViewModel = koinViewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val project = remember(projectName) {
        projects.find { it.name == projectName }
    }
    Log.d(
        "StreakDisplay",
        "ProjectStreakDialog for project: $projectName, found project data: ${project != null}"
    )
    if (project != null) {
        Log.d(
            "StreakDisplay",
            "Opening ProjectStreakDialog for project: ${project.name} with color: ${project.color}"
        )
        val projectColor = getProjectColor(project)
        val totalHours: Int = project.dailyDurationInSeconds.values.sum() / 3600
        Box(
            modifier = Modifier
                .clip(
                    MaterialTheme.shapes.large
                )
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.7f))
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (backToAggregateStreakDialog != null) {
                IconButton(
                    onClick = backToAggregateStreakDialog,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = project.name.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = projectColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        top = 8.dp,
                        start = if (backToAggregateStreakDialog != null) 24.dp else 0.dp,
                        end = if (backToAggregateStreakDialog != null) 24.dp else 0.dp
                    )
                )

                Box(
                    modifier = Modifier.offset(x = 8.dp)
                ) {
                    HourCountBadge(totalHours, 32, true)
                }

                Text(
                    text = "$totalHours Hrs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                HourMilestoneIndicator(
                    color = projectColor, totalHours
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val dailyStreakData = getProjectTargetStreak(project, TimePeriod.DAY)
                    // daily streak
                    StreakStatsDisplay(
                        label = "Daily",
                        streak = dailyStreakData.streak,
                        target = dailyStreakData.target,
                        completion = dailyStreakData.completion,
                        color = projectColor,
                        cheatLabel = "Cheat Days",
                        numCheatPeriods = 2,
                        cheatPeriodCompletion = 0.5f,
                    ) {
                    }

                    val weeklyStreakData = getProjectTargetStreak(project, TimePeriod.WEEK)
                    // weekly streak
                    StreakStatsDisplay(
                        label = "Weekly",
                        streak = weeklyStreakData.streak,
                        target = weeklyStreakData.target,
                        completion = weeklyStreakData.completion,
                        color = projectColor,
                        cheatLabel = "Cheat Weeks",
                        numCheatPeriods = 1,
                        cheatPeriodCompletion = 0.25f,
                    ) {
                    }
                }
            }
        }
    } else {
        Log.e("StreakDisplay", "ProjectStreakDialog: Project data for $projectName is null")
        Box(
            modifier = Modifier
                .clip(
                    MaterialTheme.shapes.large
                )
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.7f))
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Project data not found",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
        }
    }
}

// ? ........................
// endregion ........................

