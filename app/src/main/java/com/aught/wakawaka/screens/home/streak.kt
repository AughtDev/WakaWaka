package com.aught.wakawaka.screens.home

import com.aught.wakawaka.screens.badges.HourCountBadge
import com.aught.wakawaka.screens.badges.MILESTONES
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aught.wakawaka.data.CheatType
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.TargetStreakData
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataTransformers
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.screens.badges.getMilestoneIndex
import org.koin.androidx.compose.koinViewModel
import scrollBlurEffects
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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
                    projectName,
                    if (uiState.selectedProjectName != WakaHelpers.ALL_PROJECTS_ID) null else {
                        { activeDialog = ActiveStreakDialog.Aggregate }
                    },
                )
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

    val duration = WakaDataTransformers.calcOffsetPeriodicDurationInSeconds(
        project.dailyDurationInSeconds,
        TimePeriod.DAY,
        0
    )
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
    val hoursSuffix = if (hours == 1) "hr" else "hrs"
    val minutes = ((target - hours) * 60).roundToInt()
    val minutesSuffix = if (minutes == 1) "min" else "mins"
    return when {
        hours > 0 && minutes > 0 -> "$hours $hoursSuffix $minutes $minutesSuffix"
        hours > 0 -> "$hours $hoursSuffix"
        minutes > 0 -> "$minutes $minutesSuffix"
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

        if (cheatLabel.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = cheatLabel.uppercase(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary.copy(0.5f),
                    fontWeight = FontWeight.SemiBold,
                )
                StreakValueDisplay(
                    streak = numCheatPeriods,
                    completion = cheatPeriodCompletion,
                    color = MaterialTheme.colorScheme.tertiary,
                    textSize = 20,
                    xPadding = 12,
                    yPadding = 6
                )

                Text("USE", modifier = Modifier.clickable(true) {
                    useCheatDay()
                })
            }
        }
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

    // Track current screen state
    var currentScreen by remember { mutableStateOf<CheatDialogScreen>(CheatDialogScreen.StreakView) }

    // Get used cheat dates
    val dailyUsedCheatDates by viewModel.getUsedCheatDates(CheatType.DAILY)
        .collectAsState(initial = emptyList())
    val weeklyUsedCheatDates by viewModel.getUsedCheatDates(CheatType.WEEKLY)
        .collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.7f)),
        contentAlignment = Alignment.Center
    ) {
        when (val screen = currentScreen) {
            is CheatDialogScreen.StreakView -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // daily streak with cheat picker
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            StreakStatsDisplay(
                                label = "Daily",
                                streak = uiState.dailyTargetStreakData.streak,
                                target = uiState.dailyTargetStreakData.target,
                                completion = uiState.dailyTargetStreakData.completion,
                                color = MaterialTheme.colorScheme.primary,
                                cheatLabel = if (uiState.cheatCounts == null) "" else "Cheat Days",
                                numCheatPeriods = uiState.cheatCounts?.dailyAvailable ?: 0,
                                cheatPeriodCompletion = uiState.cheatCounts?.dailyCompletion ?: 0f
                            ) {
                                if ((uiState.cheatCounts?.dailyAvailable ?: 0) > 0) {
                                    currentScreen = CheatDialogScreen.CheatPicker(CheatType.DAILY)
                                }
                            }
                        }

                        // weekly streak with cheat picker
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            StreakStatsDisplay(
                                label = "Weekly",
                                streak = uiState.weeklyTargetStreakData.streak,
                                target = uiState.weeklyTargetStreakData.target,
                                completion = uiState.weeklyTargetStreakData.completion,
                                color = MaterialTheme.colorScheme.primary,
                                cheatLabel = if (uiState.cheatCounts == null) "" else "Cheat Weeks",
                                numCheatPeriods = uiState.cheatCounts?.weeklyAvailable ?: 0,
                                cheatPeriodCompletion = uiState.cheatCounts?.weeklyCompletion ?: 0f
                            ) {
                                if ((uiState.cheatCounts?.weeklyAvailable ?: 0) > 0) {
                                    currentScreen = CheatDialogScreen.CheatPicker(CheatType.WEEKLY)
                                }
                            }
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
                                            val dailyStreakData =
                                                getProjectTargetStreak(it, TimePeriod.DAY)
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
                                            val weeklyStreakData =
                                                getProjectTargetStreak(it, TimePeriod.WEEK)
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
                    }
                }
            }

            is CheatDialogScreen.CheatPicker -> {
                CheatCalendarPicker(
                    cheatType = screen.cheatType,
                    availableCheats = if (screen.cheatType == CheatType.DAILY)
                        uiState.cheatCounts?.dailyAvailable ?: 0
                    else
                        uiState.cheatCounts?.weeklyAvailable ?: 0,
                    usedCheatDates = if (screen.cheatType == CheatType.DAILY)
                        dailyUsedCheatDates
                    else
                        weeklyUsedCheatDates,
                    dateToDurationMap = uiState.dateToDurationMap,
                    dailyTargetHours = uiState.dailyTargetStreakData.target,
                    weeklyTargetHours = uiState.weeklyTargetStreakData.target,
                    color = MaterialTheme.colorScheme.primary,
                    onBack = { currentScreen = CheatDialogScreen.StreakView },
                    onUseCheat = { date ->
                        viewModel.useCheat(screen.cheatType, date) { success ->
                            if (success) {
                                currentScreen = CheatDialogScreen.StreakView
                            }
                        }
                    }
                )
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
    // Track current screen state
    var currentScreen by remember { mutableStateOf<CheatDialogScreen>(CheatDialogScreen.StreakView) }

    // Get used cheat dates for the project
    val dailyUsedCheatDates by viewModel.getUsedCheatDates(CheatType.DAILY, projectName)
        .collectAsState(initial = emptyList())
    val weeklyUsedCheatDates by viewModel.getUsedCheatDates(CheatType.WEEKLY, projectName)
        .collectAsState(initial = emptyList())

    val cheatCounts by viewModel.getCheatCounts(projectName)
        .collectAsState(initial = null)

    Log.d(
        "StreakDisplay",
        "ProjectStreakDialog for project: $projectName, found project data: ${project != null}"
    )
    if (project != null) {
        Log.d(
            "StreakDisplay",
            "Opening ProjectStreakDialog for project: ${project.name} with color: ${project.color} and cheat counts ${cheatCounts}"
        )
        val projectColor = getProjectColor(project)
        val totalHours: Int = project.dailyDurationInSeconds.values.sum() / 3600
        val dailyStreakData = getProjectTargetStreak(project, TimePeriod.DAY)
        val weeklyStreakData = getProjectTargetStreak(project, TimePeriod.WEEK)

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.7f))
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val screen = currentScreen) {
                is CheatDialogScreen.StreakView -> {
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
                            // daily streak
                            StreakStatsDisplay(
                                label = "Daily",
                                streak = dailyStreakData.streak,
                                target = dailyStreakData.target,
                                completion = dailyStreakData.completion,
                                color = projectColor,
                                cheatLabel = if (cheatCounts == null) "" else "Cheat Days",
                                numCheatPeriods = cheatCounts?.dailyAvailable ?: 0,
                                cheatPeriodCompletion = cheatCounts?.dailyCompletion ?: 0f
                            ) {
                                if ((cheatCounts?.dailyAvailable ?: 0) > 0) {
                                    currentScreen = CheatDialogScreen.CheatPicker(CheatType.DAILY)
                                }
                            }

                            // weekly streak
                            StreakStatsDisplay(
                                label = "Weekly",
                                streak = weeklyStreakData.streak,
                                target = weeklyStreakData.target,
                                completion = weeklyStreakData.completion,
                                color = projectColor,
                                cheatLabel = if (cheatCounts == null) "" else "Cheat Weeks",
                                numCheatPeriods = cheatCounts?.weeklyAvailable ?: 0,
                                cheatPeriodCompletion = cheatCounts?.weeklyCompletion ?: 0f
                            ) {
                                if ((cheatCounts?.weeklyAvailable ?: 0) > 0) {
                                    currentScreen = CheatDialogScreen.CheatPicker(CheatType.WEEKLY)
                                }
                            }
                        }
                    }
                }

                is CheatDialogScreen.CheatPicker -> {
                    CheatCalendarPicker(
                        cheatType = screen.cheatType,
                        availableCheats = if (screen.cheatType == CheatType.DAILY)
                            cheatCounts?.dailyAvailable ?: 0
                        else
                            cheatCounts?.weeklyAvailable ?: 0,
                        usedCheatDates = if (screen.cheatType == CheatType.DAILY)
                            dailyUsedCheatDates
                        else
                            weeklyUsedCheatDates,
                        dateToDurationMap = project.dailyDurationInSeconds,
                        dailyTargetHours = dailyStreakData.target,
                        weeklyTargetHours = weeklyStreakData.target,
                        color = projectColor,
                        onBack = { currentScreen = CheatDialogScreen.StreakView },
                        onUseCheat = { date ->
                            viewModel.useCheat(screen.cheatType, date, projectName) { success ->
                                if (success) {
                                    currentScreen = CheatDialogScreen.StreakView
                                }
                            }
                        }
                    )
                }
            }
        }
    } else {
        Log.e("StreakDisplay", "ProjectStreakDialog: Project data for $projectName is null")
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
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


// region CHEAT CALENDAR PICKER
// ? ........................

sealed class CheatDialogScreen {
    object StreakView : CheatDialogScreen()
    data class CheatPicker(val cheatType: CheatType) : CheatDialogScreen()
}

@Composable
fun CheatCalendarPicker(
    cheatType: CheatType,
    availableCheats: Int,
    usedCheatDates: List<String>,
    dateToDurationMap: Map<String, Int>,
    dailyTargetHours: Float?,
    weeklyTargetHours: Float?,
    color: Color,
    onBack: () -> Unit,
    onUseCheat: (LocalDate) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val today = LocalDate.now()

    // For weekly cheats, we need to track the selected week
    val selectedWeekStart = selectedDate?.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Calculate blocked dates (where target was met)
    val blockedDates = remember(dateToDurationMap, cheatType, dailyTargetHours, weeklyTargetHours) {
        val blocked = mutableSetOf<String>()

        when (cheatType) {
            CheatType.DAILY -> {
                // Block dates where daily target was met
                dateToDurationMap.forEach { (dateStr, duration) ->
                    val targetSeconds = (dailyTargetHours ?: 0f) * 3600
                    if (duration >= targetSeconds) {
                        blocked.add(dateStr)
                    }
                }
            }

            CheatType.WEEKLY -> {
                // Block weeks where weekly target was met
                val weeklyDurations = mutableMapOf<String, Int>()
                dateToDurationMap.forEach { (dateStr, duration) ->
                    val date = LocalDate.parse(dateStr)
                    val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weekKey = weekStart.format(dateFormatter)
                    weeklyDurations[weekKey] = (weeklyDurations[weekKey] ?: 0) + duration
                }
                weeklyDurations.forEach { (weekKey, duration) ->
                    val targetSeconds = (weeklyTargetHours ?: 0f) * 3600
                    if (duration >= targetSeconds) {
                        blocked.add(weekKey)
                    }
                }
            }
        }
        blocked
    }

    // Get days in current month
    val daysInMonth = remember(currentMonth) {
        val firstDay = currentMonth.atDay(1)
        val lastDay = currentMonth.atEndOfMonth()
        val startOffset = (firstDay.dayOfWeek.value - 1) // Monday = 0

        buildList {
            // Add empty cells for days before the month starts
            repeat(startOffset) { add(null) }
            // Add all days in the month
            var day = firstDay
            while (!day.isAfter(lastDay)) {
                add(day)
                day = day.plusDays(1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back"
                )
            }
            Text(
                text = if (cheatType == CheatType.DAILY) "Use Cheat Day" else "Use Cheat Week",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Available cheats info
        Text(
            text = "$availableCheats ${if (cheatType == CheatType.DAILY) "day" else "week"}${if (availableCheats != 1) "s" else ""} available",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = { currentMonth = currentMonth.plusMonths(1) },
                enabled = currentMonth.isBefore(YearMonth.now())
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (currentMonth.isBefore(YearMonth.now()))
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day of week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(daysInMonth) { day ->
                if (day == null) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val dateStr = day.format(dateFormatter)
                    val weekStartStr = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .format(dateFormatter)

                    val isBlocked = when (cheatType) {
                        CheatType.DAILY -> blockedDates.contains(dateStr) || usedCheatDates.contains(
                            dateStr
                        )

                        CheatType.WEEKLY -> blockedDates.contains(weekStartStr) || usedCheatDates.contains(
                            weekStartStr
                        )
                    }

                    val isUsedCheat = when (cheatType) {
                        CheatType.DAILY -> usedCheatDates.contains(dateStr)
                        CheatType.WEEKLY -> usedCheatDates.contains(weekStartStr)
                    }

                    val isFuture = day.isAfter(today)
                    val isSelectable = !isBlocked && !isFuture

                    val isSelected = when (cheatType) {
                        CheatType.DAILY -> selectedDate == day
                        CheatType.WEEKLY -> selectedWeekStart != null &&
                                day >= selectedWeekStart &&
                                day < selectedWeekStart.plusDays(7)
                    }

                    val isInSelectedWeek = cheatType == CheatType.WEEKLY && isSelected

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> color.copy(0.8f)
                                    isUsedCheat -> MaterialTheme.colorScheme.tertiary.copy(0.3f)
                                    isBlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (isSelectable) {
                                    Modifier.clickable { selectedDate = day }
                                } else Modifier
                            )
                            .then(
                                if (day == today) {
                                    Modifier.border(1.dp, color, CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.dayOfMonth.toString(),
                            fontSize = 12.sp,
                            color = when {
                                isSelected -> Color.White
                                isFuture -> MaterialTheme.colorScheme.onSurface.copy(0.2f)
                                isBlocked -> MaterialTheme.colorScheme.onSurface.copy(0.3f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                label = "Target met"
            )
            LegendItem(
                color = MaterialTheme.colorScheme.tertiary.copy(0.3f),
                label = "Cheat used"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm button
        Button(
            onClick = {
                selectedDate?.let { date ->
                    onUseCheat(date)
                }
            },
            enabled = selectedDate != null && availableCheats > 0,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = if (selectedDate != null) {
                    val displayDate = when (cheatType) {
                        CheatType.DAILY -> selectedDate!!.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                        CheatType.WEEKLY -> {
                            val weekStart =
                                selectedDate!!.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            val weekEnd = weekStart.plusDays(6)
                            "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} - ${
                                weekEnd.format(
                                    DateTimeFormatter.ofPattern("MMM d")
                                )
                            }"
                        }
                    }
                    "Use cheat for $displayDate"
                } else {
                    "Select a ${if (cheatType == CheatType.DAILY) "day" else "week"}"
                },
                fontSize = 14.sp
            )
        }

        if (selectedDate != null && availableCheats > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You will have ${availableCheats - 1} ${if (cheatType == CheatType.DAILY) "day" else "week"}${if (availableCheats - 1 != 1) "s" else ""} left",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
        )
    }
}

// ? ........................
// endregion ........................
