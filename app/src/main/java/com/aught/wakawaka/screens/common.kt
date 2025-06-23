import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aught.wakawaka.data.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTargetCard(
    dailyTarget: Float,
    withDailyTarget: Boolean,
    dailyStreakExcludedDays: List<Int>,
    onDailyTargetChange: (Float) -> Unit,
    onExcludedDaysChange: (List<Int>) -> Unit,
    onWithDailyTargetChange: (Boolean) -> Unit
) {
    // Daily Target Section
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Daily Target")
                    Text(
                        text = "Daily Target",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Checkbox(
                    checked = withDailyTarget,
                    onCheckedChange = {
                        onWithDailyTargetChange(it)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            if (withDailyTarget) {
                Text(
                    text = "Set your daily coding goal in hours: ${
                        String.format(
                            "%.1f",
                            dailyTarget
                        )
                    } hours",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = dailyTarget,
                    onValueChange = { onDailyTargetChange(it) },
                    valueRange = 1f..12f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CalendarMonth, contentDescription = "Excluded Days",
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = "Excluded days",
                        style = MaterialTheme.typography.titleSmall
                    )

                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .weight(1f),
                            onClick = {
                                onExcludedDaysChange(
                                    if (dailyStreakExcludedDays.contains(day.index)) {
                                        dailyStreakExcludedDays.filter { it != day.index }
                                    } else {
                                        dailyStreakExcludedDays + day.index
                                    })
                            }
                        ) {
                            Text(
                                modifier = Modifier
                                    .background(
                                        if (dailyStreakExcludedDays.contains(day.index)) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                text = day.name[0].uppercase(),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyTargetCard(
    weeklyTarget: Float,
    withWeeklyTarget: Boolean,
    onWeeklyTargetChange: (Float) -> Unit,
    onWithWeeklyTargetChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Weekly Target")
                    Text(
                        text = "Weekly Target",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Checkbox(
                    checked = withWeeklyTarget,
                    onCheckedChange = {
                        onWithWeeklyTargetChange(it)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            if (withWeeklyTarget) {
                Text(
                    text = "Set your weekly coding goal in hours: ${
                        String.format(
                            "%.1f",
                            weeklyTarget
                        )
                    } hours",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = weeklyTarget,
                    onValueChange = {
                        onWeeklyTargetChange(it)
                    },
                    valueRange = 5f..75f,
                    steps = 69,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

}


enum class AlertType(val status: String) {
    Success("success"),
    Failure("failure"),
}

data class AlertData(
    val message: String,
    val type: AlertType = AlertType.Success,
)

@Composable
fun AlertPane(alertData: AlertData?, isVisible: Boolean) {
    if (alertData != null) {
        val tint = if (alertData.type == AlertType.Success) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }
        val containerColor = if (alertData.type == AlertType.Success) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        AnimatedVisibility(visible = isVisible) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = alertData.type.status,
                        tint = tint
                    )
                    Text(
                        text = alertData.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tint
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
@Composable
fun ScrollBlurEffects(
    lazyListState: LazyListState,
    weeklyDataSize: Int
) {

    // top blur opacity
    val topBlurOpacity by remember {
        // if the first item is not visible, then the opacity is 1, else the opacity is
        // the firstItemOffset divided by the height of the first item
        derivedStateOf {
            val firstItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstItem == null) {
                return@derivedStateOf 1f
            }
            if (lazyListState.firstVisibleItemIndex != 0) {
                1f
            } else {
                lazyListState.firstVisibleItemScrollOffset.toFloat() / lazyListState.layoutInfo.visibleItemsInfo.first().size.toFloat()
            }
        }
    }

    // bottom blur opacity
    val bottomBlurOpacity by remember(weeklyDataSize) {
        derivedStateOf {
            val lastItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastItem == null) {
                return@derivedStateOf 1f;
            }
            val distanceToBottom =
                (lastItem.offset + lastItem.size - lazyListState.layoutInfo.viewportEndOffset)
//            Log.d("waka", "lastItem: ${lastItem.index}, ${lastItem.size}, distToBottom $distanceToBottom , weekly data size is $weeklyDataSize")
            if (lastItem.index != weeklyDataSize - 1) {
                1f
            } else {
                distanceToBottom.toFloat() / lastItem.size.toFloat()
            }
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = topBlurOpacity),
                                Color.Transparent
                            )
                        )
                    )
            )


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = bottomBlurOpacity),
                            )
                        )
                    )
            )
        }
    }
}

