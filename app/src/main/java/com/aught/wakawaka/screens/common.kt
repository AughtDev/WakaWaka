import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aught.wakawaka.data.DayOfWeek
import com.aught.wakawaka.utils.ColorUtils

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
    weeklyDataSize: Int,
    color: Color = MaterialTheme.colorScheme.background,
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
                                color.copy(alpha = topBlurOpacity),
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
                                color.copy(alpha = bottomBlurOpacity),
                            )
                        )
                    )
            )
        }
    }
}


// region HOUR MILESTONE BADGES
// ? ........................

data class Milestone(
    val hours: Int,
    val colorHex: String,
    val crown: DrawScope.(Color, Int) -> Unit
)

val MILESTONES: List<Milestone> = listOf(
    Milestone(25, "#B8702E", DrawScope::bronzeCrown),
    Milestone(50, "#C0C0C0", DrawScope::silverCrown),
    Milestone(100, "#CFB53B", DrawScope::goldCrown),
    Milestone(250, "#00A693", DrawScope::diamondCrown),
    Milestone(500, "#9F7FF5", DrawScope::royalPurpleCrown),
    Milestone(1000, "#F56058", DrawScope::royalRedCrown),
)

// region CROWNS
// ? ........................

fun DrawScope.goldCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadraticTo(w / 2, sz + 1 + yOff, w - sw, sw + yOff)
        lineTo(w - sw, sz + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        ),
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(sw, -sw * 1 + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw * 2 + yOff),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w - sw, -sw * 1 + yOff),
        style = Fill
    )

}

fun DrawScope.bronzeCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw * 3 / 2f, sz * 3 / 4f + yOff)
        quadraticTo(w / 2, sz * 3 / 4f + yOff, w / 2, sw + yOff)
        quadraticTo(w / 2, sz * 3 / 4f + yOff, w - sw * 3 / 2f, sz * 3 / 4f + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw / 2),
        style = Fill
    )
}

fun DrawScope.silverCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw * 3 / 2f, sz * 3 / 4f + yOff)
        lineTo(sw * 3 / 2f, sw + yOff)
        quadraticTo(w / 2, sz + 1 + yOff, w - sw * 3 / 2f, sw + yOff)
        lineTo(w - sw * 3 / 2f, sz * 3 / 4f + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw + yOff),
        style = Fill
    )
}

fun DrawScope.diamondCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadraticTo(w / 4, sz + yOff, w / 2, 0f + yOff)
        quadraticTo(3 * w / 4, sz + yOff, w - sw, sw + yOff)
        lineTo(w - sw, sz + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w - sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
}

fun DrawScope.royalPurpleCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadraticTo(w / 4, sz + yOff, w / 2, 0f + yOff)
        quadraticTo(3 * w / 4, sz + yOff, w - sw, sw + yOff)
        lineTo(w - sw, sz + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw * 3f + yOff),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w - sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
}

fun DrawScope.royalRedCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw / 2, sz + yOff)
        lineTo(sw / 2, sw * 2 + yOff)
        quadraticTo(w / 8, sz + yOff, w / 4, sw * 3 / 2 + yOff)
        quadraticTo(w / 2, sz + yOff, 3 * w / 4, sw + yOff)
        quadraticTo(7 * w / 8, sz + yOff, w - sw / 2, sw * 3 / 2 + yOff)
        lineTo(w - sw / 2, sz + yOff)
    }
    val gemPath = Path().apply {
        moveTo(w / 2 - sw, -sz / 2 + yOff)
        lineTo(w / 2, -sz * 4 / 5f + yOff)
        lineTo(w / 2 + sw, -sz / 2 + yOff)
        lineTo(w / 2, -sz * 1 / 5f + yOff)
        close()
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawPath(
        path = gemPath,
        color = color.copy(alpha = 0.7f),
        style = Stroke(
            width = sw * 2 / 3f,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw * 2 / 3f,
        center = Offset(sw, -sw + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = sw,
        center = Offset(w / 4, -sw * 4 / 3f + yOff),
        style = Stroke(
            width = sw * 2 / 3f,
            cap = StrokeCap.Round
        )
//        style=Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = sw,
        center = Offset(3 * w / 4, -sw * 4 / 3f + yOff),
        style = Stroke(
            width = sw * 2 / 3f,
            cap = StrokeCap.Round
        )
//                style=Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw * 2 / 3f,
        center = Offset(w - sw, -sw + yOff),
        style = Fill
    )
}


// ? ........................
// endregion ........................

@Composable
fun BadgeText(
    hours: Int,
    color: Color,
    fontSize: Int,
    animated: Boolean,
    drawBehind: DrawScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sheen_transition")

    val sheenTranslate = if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3500,
                    // with random delay to avoid all badges shining at the same time
                    delayMillis = 500,
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "sheen_translate"
        ).value
    } else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.shiny(color, sheenTranslate)
//        modifier = Modifier.drawBehind(drawBehind)
    ) {
        Text(
            text = "$hours",
            color = color.copy(alpha = 0.7f),
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.8f).sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 8.dp)
                .drawBehind(drawBehind)
        )
        Text(
            text = "h",
            color = color.copy(alpha = 0.5f),
            fontSize = (fontSize * 2 / 3).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = 5.dp, x = 0.dp)
        )
    }
}

fun DrawScope.mainUnderline(color: Color, size: Int) {
    val offset = size * 2 / 3f
    drawLine(
        color.copy(alpha = 0.3f),
        start = Offset(
            x = 10f,
            y = this.size.height - offset
        ),
        end = Offset(
            x = this.size.width - 10f,
            y = this.size.height - offset
        ),
        strokeWidth = size / 3f,
        cap = StrokeCap.Round
    )
}


fun Modifier.shiny(badgeColor: Color, translate: Float) = this.drawWithCache {
    // The sheen will be a diagonal gradient
    val sheenBrush = Brush.linearGradient(
        colors = listOf(
            badgeColor.copy(0.5f),
            badgeColor.copy(0.5f),
            badgeColor.copy(0.4f),
            badgeColor,
            badgeColor.copy(0.4f),
            badgeColor.copy(0.5f),
            badgeColor.copy(0.5f),
        ),
        start = Offset(0f + size.width * translate, 0f),
        end = Offset(size.width * 2 + size.width * translate, size.height / 2)
    )


    val paint = Paint()

    val layerBounds = Rect(Offset.Zero, size)

    onDrawWithContent {
        // 1. Draw the original content (the badge) into a layer
        drawIntoCanvas { canvas ->
            canvas.saveLayer(layerBounds, paint)
            // This is where the original @Composable content goes
            drawContent()

            val sheenPaint = Paint().apply {
                this.blendMode = BlendMode.SrcIn
            }

            canvas.saveLayer(layerBounds, sheenPaint)

            // 2. Draw the sheen on top, using SrcIn to mask it
            // to the badge's shape.
            drawRect(
                brush = sheenBrush,
            )

            canvas.restore()

            canvas.restore()
        }
    }
}

fun getMilestoneIndex(totalHours: Int): Int {
    var milestoneIndex = 0
    while (milestoneIndex < MILESTONES.size && totalHours >= MILESTONES[milestoneIndex].hours) {
        milestoneIndex++
    }
    return milestoneIndex - 1
}

@Composable
fun HourCountBadge(hours: Int, size: Int = 14, animated: Boolean = false) {
    Box() {
        if (hours < 25) null
        else {
            val milestoneIndex = getMilestoneIndex(hours)
            val milestone = MILESTONES[milestoneIndex]
            val col = ColorUtils.hexToColor(milestone.colorHex)
            BadgeText(milestone.hours, col, size, animated) {
                mainUnderline(col, size)
                milestone.crown(this, col, size)
            }
        }
    }
}

// ? ........................
// endregion ........................


fun Modifier.scrollBlurEffects(
    lazyListState: LazyListState,
    weeklyDataSize: Int,
//    topBlurOpacity: Float,
//    bottomBlurOpacity: Float,
    blurProportion: Float = 0.3f,
) = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithContent {
        // top blur opacity
        val topBlurOpacity =
        // if the first item is not visible, then the opacity is 1, else the opacity is
            // the firstItemOffset divided by the height of the first item
            if (lazyListState.layoutInfo.visibleItemsInfo.firstOrNull() == null ||
                lazyListState.firstVisibleItemIndex != 0
            ) 1f else {
                lazyListState.firstVisibleItemScrollOffset.toFloat() / lazyListState.layoutInfo.visibleItemsInfo.first().size.toFloat()
            }

        val lastItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
        // bottom blur opacity
        val bottomBlurOpacity =
            if (lastItem == null || lastItem.index != weeklyDataSize - 1) 1f else {
                val distanceToBottom =
                    (lastItem.offset + lastItem.size - lazyListState.layoutInfo.viewportEndOffset)
                distanceToBottom.toFloat() / lastItem.size.toFloat()
            }

//        Log.d(
//            "scrollBlur",
//            "topBlurOpacity: $topBlurOpacity, bottomBlurOpacity: $bottomBlurOpacity, firstVisibleItemIndex: ${lazyListState.firstVisibleItemIndex}, firstVisibleItemScrollOffset: ${lazyListState.firstVisibleItemScrollOffset}, lastItemIndex: ${lastItem?.index}, distanceToBottom: ${if (lastItem != null) (lastItem.offset + lastItem.size - lazyListState.layoutInfo.viewportEndOffset) else "N/A"}"
//        )

        drawContent()
        val gradient = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Black.copy(1 - topBlurOpacity),
                blurProportion to Color.Black,
                1 - blurProportion to Color.Black,
                1.0f to Color.Black.copy(1 - bottomBlurOpacity),
            ),
        )
        drawRect(
            brush = gradient,
            blendMode = BlendMode.DstIn
        )
    }
