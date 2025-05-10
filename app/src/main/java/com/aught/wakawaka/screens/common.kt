import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        modifier = Modifier.fillMaxWidth().animateContentSize()
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
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Checkbox(
                    checked = withDailyTarget,
                    onCheckedChange = {
                        onWithDailyTargetChange(it)
                    }
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
        modifier = Modifier.fillMaxWidth().animateContentSize()
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
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Checkbox(
                    checked = withWeeklyTarget,
                    onCheckedChange = {
                        onWithWeeklyTargetChange(it)
                    }
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
                    steps = 34,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

}
