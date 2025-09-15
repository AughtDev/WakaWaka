package com.aught.wakawaka.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DurationStats
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.workers.WakaDataFetchWorker
import kotlin.math.roundToInt


// region IMAGE GENERATION
// ? ........................


// ? ........................
// endregion ........................


// region DIALOG
// ? ........................

@Composable
fun SharePane(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val paneSize = 65
    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(true) {
                onClick()
            }
            .padding(4.dp)
        ,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // sample image
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = Modifier
                .height(paneSize.dp)
                .fillMaxWidth(0.7f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                modifier = Modifier
            )
        }
        Box(
            modifier = Modifier
                .size(paneSize.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
        }
    }
}

fun shareImage(context: Context, uri: Uri?) {
    if (uri == null) return

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share post to..."))
}

enum class CaptureMode {
    SUMMARY, CALENDAR, NONE
}

@Composable
fun ShareDialog(
    context: Context,
    selectedProject: String,
    wakaDataHandler: WakaDataHandler
) {
    val isAggregate = remember {
        selectedProject == WakaHelpers.ALL_PROJECTS_ID
    }

    val label = remember {
        if (isAggregate) "aggregate" else selectedProject
    }

    val dateToDurationInSeconds: Map<String, Int> = remember {
        if (isAggregate) {
            wakaDataHandler.aggregateData?.dailyRecords?.mapValues {
                it.value.totalSeconds
            } ?: emptyMap()
        } else {
            wakaDataHandler.projectSpecificData[selectedProject]?.dailyDurationInSeconds
                ?: emptyMap()
        }
    }

    var captureMode by remember {
        mutableStateOf(CaptureMode.NONE)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier.height(0.dp)
        )
        SharePane(
            "Share Summary Image",
            "Share your summary $label coding stats as an image",
        ) {
            captureMode = CaptureMode.SUMMARY
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.7f),
            color = MaterialTheme.colorScheme.onSurface.copy(0.1f)
        )

        SharePane(
            "Share Calendar Image",
            "Share your $label coding activity calendar as an image",
        ) {
            captureMode = CaptureMode.CALENDAR
        }
        Spacer(
            modifier = Modifier.height(0.dp)
        )
    }

    when (captureMode) {

        CaptureMode.SUMMARY -> {
            val dataRequest = if (isAggregate) {
                DataRequest.Aggregate
            } else {
                DataRequest.ProjectSpecific(selectedProject)
            }
            val dailyStreakData = ImageStreakData(
                    target = wakaDataHandler.getTarget(dataRequest, TimePeriod.DAY),
                    streak = wakaDataHandler.getStreak(dataRequest, TimePeriod.DAY).count,
                    completion = wakaDataHandler.getStreakCompletion(dataRequest, TimePeriod.DAY)
                )

            val weeklyStreakData = ImageStreakData(
                target = wakaDataHandler.getTarget(dataRequest, TimePeriod.WEEK),
                streak = wakaDataHandler.getStreak(dataRequest, TimePeriod.WEEK).count,
                completion = wakaDataHandler.getStreakCompletion(dataRequest, TimePeriod.WEEK)
            )

            val stats = WakaDataFetchWorker.loadWakaStatistics(context)

            val durationStats = if (isAggregate) {
                stats.aggregateStats
            } else {
                stats.projectStats[selectedProject] ?: DurationStats(0, 0, 0, 0, 0)
            }

            val statToDurationInSeconds = listOf(
                    "Today" to durationStats.today,
                    "This Week" to wakaDataHandler.getOffsetPeriodicDurationInSeconds(
                        dataRequest,
                        TimePeriod.WEEK,
                        0
                    ),
                    "Past 30 Days" to durationStats.last30Days,
                    "Past Year" to durationStats.lastYear,
                    "All Time" to durationStats.allTime
                )

            val uri = generateSummaryCardImage(
                context, if (isAggregate) "Aggregate" else selectedProject,
                totalHours = (dateToDurationInSeconds.values.sum() / 3600f),
                dailyStreakData = dailyStreakData,
                weeklyStreakData = weeklyStreakData,
                statToDurationInSeconds = statToDurationInSeconds,
                ImageColors(
                    background = MaterialTheme.colorScheme.surfaceContainerLowest,
                    foreground = MaterialTheme.colorScheme.onSurface,
                    primary = if (isAggregate) MaterialTheme.colorScheme.primary else wakaDataHandler.getProjectColor(
                        selectedProject
                    ),
                    secondary = MaterialTheme.colorScheme.tertiary
                )
            )
            shareImage(context, uri)
            captureMode = CaptureMode.NONE
        }

        CaptureMode.CALENDAR -> {
            val projectColor = if (isAggregate) {
                MaterialTheme.colorScheme.primary
            } else {
                wakaDataHandler.getProjectColor(selectedProject)
            }
            Log.d("ShareDialog", "Project color: $projectColor")
            val uri = generateCalendarShareImage(
                context, if (isAggregate) null else selectedProject,
                ImageColors(
                    background = MaterialTheme.colorScheme.surfaceContainerLowest,
                    foreground = MaterialTheme.colorScheme.onSurface,
                    primary = projectColor,
                    secondary = MaterialTheme.colorScheme.tertiary
                ),
                dateToDurationInSeconds
            )
            shareImage(context, uri)
            captureMode = CaptureMode.NONE
        }

        else -> {}
    }
}

// ? ........................
// endregion ........................


@Composable
fun ShareButton(
    context: Context,
    selectedProject: String,
    wakaDataHandler: WakaDataHandler,
    modifier: Modifier = Modifier
) {

    var shareDialogOpen by remember {
        mutableStateOf(false)
    }

    IconButton(
        onClick = {
            shareDialogOpen = true
        },
        modifier = modifier,
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = "Share"
        )
    }

    if (shareDialogOpen) {
        Dialog(
            onDismissRequest = {
                shareDialogOpen = false
            }
        ) {
            ShareDialog(context, selectedProject, wakaDataHandler)
        }
    }
}
