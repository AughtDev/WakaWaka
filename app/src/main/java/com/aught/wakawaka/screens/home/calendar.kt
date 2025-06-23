package com.aught.wakawaka.screens.home

import ScrollBlurEffects
import android.R
import android.graphics.BlurMaskFilter
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.utils.ColorUtils
import java.time.LocalDate
import kotlin.collections.forEach
import kotlin.math.max

data class DayData(
    val date: Int,
    val month: String,
    val day: String,
    val year: String,
    val yyyymmdd: String,
    val durationInHours: Float,
    val isFutureDate: Boolean = false,
    val isToday: Boolean = false,
)

data class DayColorData(
    val projectColor: Color,
    val textColor: Color,
//    val luminance: Float
)

fun Modifier.glow(
    color: Color,
    cornerRadius: Dp = 0.dp,
    glowRadius: Dp = 20.dp,
    alpha: Float = 0.5f,
    xOffset: Dp = 0.dp,
    yOffset: Dp = 0.dp
): Modifier = this.drawBehind {
    // Convert Dp values to Px
    val cornerRadiusPx = cornerRadius.toPx()
    val glowRadiusPx = glowRadius.toPx()
    val xOffsetPx = xOffset.toPx()
    val yOffsetPx = yOffset.toPx()

    // Create a Paint object for the glow
    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()

    // Set the color and alpha for the glow
    frameworkPaint.color = color.copy(alpha = alpha).toArgb()

    // Apply a blur mask filter to create the glow effect
    // BlurMaskFilter.Style.NORMAL blurs the entire shape.
    frameworkPaint.maskFilter = BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL)

    // Draw the glow effect onto the canvas
    drawIntoCanvas {
        it.drawRoundRect(
            left = xOffsetPx,
            top = yOffsetPx,
            right = this.size.width + xOffsetPx,
            bottom = this.size.height + yOffsetPx,
            radiusX = cornerRadiusPx,
            radiusY = cornerRadiusPx,
            paint = paint
        )
    }
}

@Composable
fun DayCard(
    dayData: DayData,
    dayColorData: DayColorData,
    excludedDays: Set<Int> = emptySet(),
    targetInHours: Float? = null,
    opacityStops: List<Pair<Float, Float>>,
    setDialogDayData: () -> Unit,
) {
    val cellSize = 48.dp
    val progressPathPadding = 16f
    val cornerRadius = 24f
    val isFirstOfMonth = dayData.date == 1
    val isToday = dayData.isToday
    val idx = dayOfWeekStringToIdx(dayData.day)

    val path = remember {
        Path()
    }

    val pathMeasure = remember {
        PathMeasure()
    }

    var segmentPath by remember {
        mutableStateOf(Path())
    }

    val targetCompletion by remember(dayData) {
        derivedStateOf {
            if (targetInHours == null) {
                if (dayData.durationInHours > 0) 1f else 0f
            } else {
                (dayData.durationInHours / targetInHours).coerceIn(0f, 1f)
            }
        }
    }

    val cardOpacity by remember(dayData) {
        derivedStateOf {
            if (dayData.isFutureDate) {
                0f
            } else {
                calculateWindowedOpacity(
                    dayData.durationInHours,
                    opacityStops
                )
            }
        }
    }

    LaunchedEffect(dayData.durationInHours) {
        // reset the segment path if duration changes
        if (targetInHours != null) {
            segmentPath = Path()
        }
    }

    val cardModifier = Modifier
        .size(cellSize)
        .padding(3.dp)
    Card(
        modifier = cardModifier.clickable(onClick = {
            setDialogDayData()
        })
    ) {
        val bgColor = dayColorData.projectColor.copy(
            0.1f + (0.9f * cardOpacity)
        )

        if (dayData.yyyymmdd == "2025-06-23") {
            Log.d("waka", "Card opacity for ${dayData.yyyymmdd} is $cardOpacity for dayData $dayData")
        }
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val luminance =
                    WakaHelpers.PROJECT_COLOR_LUMINANCE * (cardOpacity)
//                + dayColorData.luminance * (1 - cardOpacity)
                val customTextColor = if (isToday || isFirstOfMonth || luminance < 0.4f) {
                    dayColorData.projectColor
                } else {
                    if (dayData.isFutureDate) {
                        dayColorData.textColor.copy(0.6f)
                    } else {
                        dayColorData.textColor.copy(if (isToday) 1f else 0.7f)
                    }
                }
                if (!excludedDays.contains(idx) && !dayData.isFutureDate) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
//                        modifier = if (isToday && !isFirstOfMonth) {
//                            Modifier
//                                .fillMaxSize(0.55f)
//                                .border(1.dp, customTextColor, RoundedCornerShape(6.dp))
//                        } else {
//                            Modifier.fillMaxSize()
//                        }

                    ) {
                        Text(
                            text = if (isFirstOfMonth) dayData.month.slice(0..2) else dayData.date.toString(),
                            color = customTextColor,
                            fontSize = when {
                                isFirstOfMonth -> 11.sp
//                                dayData.isToday -> 16.sp
                                else -> 14.sp
                            },
                            lineHeight = when {
                                isFirstOfMonth -> TextUnit.Unspecified
//                                dayData.isToday -> 16.sp
                                else -> 14.sp
                            },
                            textDecoration = if (isFirstOfMonth && isToday) TextDecoration.Underline else TextDecoration.None,
                            modifier = if (isFirstOfMonth || isToday) Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 3.dp, vertical = 2.dp) else Modifier,
                            fontWeight = if (isFirstOfMonth) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
//            if (isToday) {
//                Box(
//                    modifier = Modifier
//                        .height(3.dp)
//                        .width(14.dp)
//                        .offset(y = 29.dp)
//                        .clip(RoundedCornerShape(2.dp))
//                        .background(MaterialTheme.colorScheme.surface)
//                )
//            }
            if (targetInHours != null && targetCompletion == 1f) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
//                        .height(4.dp)
//                        .width(4.dp)
                        .offset(y = 4.dp)
                        .clip(CircleShape)
                        .background(dayColorData.projectColor)
                )
            }
            if (targetInHours != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                                            ), cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                        )
                                    )
                                }
                                pathMeasure.setPath(path, true)
                            }
                            segmentPath.reset()

                            val start = 0.8f
                            pathMeasure.getSegment(
//                                0f,targetCompletion * pathMeasure.length,
                                pathMeasure.length * max(0f, start - targetCompletion),
                                pathMeasure.length * start,
                                segmentPath,
                                true
                            )
                            if (targetCompletion > start) {
                                pathMeasure.getSegment(
                                    pathMeasure.length * (1f - (targetCompletion - start)),
                                    pathMeasure.length * 1f,
                                    segmentPath,
                                    true
                                )
                            }


                            drawPath(
                                segmentPath,
                                color = dayColorData.projectColor.copy(
                                    alpha = if (targetCompletion == 1f) 0.7f else 0.5f
                                ),
                                style = Stroke(
                                    width = 4f,
                                    pathEffect = null,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                ) {

                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekGraph(
    data: List<DayData>,
    targetInHours: Float? = null,
    textColor: Color,
    projectColor: Color,
    excludedDays: Set<Int>,
    opacityStops: List<Pair<Float, Float>>,
    setDialogDayData: (DayData) -> Unit
) {

    val dayColorData = remember(projectColor) {
        DayColorData(
            projectColor = projectColor,
            textColor = textColor,
//            luminance = ColorUtils.calculateLuminance(projectColor)
        )
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        (0..6).forEach {
            DayCard(
                dayData = data[it],
                dayColorData = dayColorData,
                excludedDays = excludedDays,
                targetInHours = targetInHours,
                opacityStops = opacityStops,
                setDialogDayData = { setDialogDayData(data[it]) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGraph(
    projectName: String,
    dateToDurationMap: Map<String, Int>,
    targetInHours: Float?,
    projectColor: Color,
    aggregateData: AggregateData? = null
) {
//    Log.d("waka", "Generating calendar graph for project: $projectName")
    var showDialog by remember { mutableStateOf(false) }
    var dialogDayData by remember { mutableStateOf<DayData?>(null) }

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                dialogDayData = null
            }
        ) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val dialogTextColor = Color.White
                // date string of dialog day data of the format 15th May 2025
                val dateString = dialogDayData?.let {
                    "${it.date}${WakaHelpers.getDateSuffix(it.date)} ${it.month} ${it.year}"
                } ?: "No date selected"
                val durationString = dialogDayData?.let {
                    WakaHelpers.durationInSecondsToDurationString(
                        dateToDurationMap[it.yyyymmdd] ?: 0, "h", "m"
                    )
                } ?: "No duration selected"
                val projects = dialogDayData?.let {
                    if (projectName != WakaHelpers.ALL_PROJECTS_ID) {
                        null
                    } else {
                        aggregateData?.dailyRecords[it.yyyymmdd]?.projects
                    }
                }?.sortedByDescending { it.totalSeconds }
                val dayString = dialogDayData?.day?.uppercase() ?: "No day selected"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollable(rememberScrollState(), Orientation.Vertical),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(dayString, fontSize = 16.sp, color = dialogTextColor)
                    Text(dateString, color = dialogTextColor)
                    if (projects != null) {
                        val textSize = 12.sp
                        val padding = 8.dp
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                projects.forEach {
                                    Text(
                                        WakaHelpers.truncateLabel(it.name.uppercase(), 16),
                                        fontSize = textSize,
                                        color = dialogTextColor
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = padding),
                                horizontalAlignment = Alignment.End
                            ) {
                                projects.forEach {
                                    Text(
                                        WakaHelpers.durationInSecondsToDurationString(it.totalSeconds),
                                        fontSize = textSize,
                                        color = dialogTextColor
                                    )
                                }
                            }
                        }
                    }
                    Text(durationString, color = dialogTextColor)
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()

    val opacityStops = remember(dateToDurationMap) {
        // only consider durations above 5 minutes
        val filteredDurations = dateToDurationMap.values.filter {
            it > 60 * 5 // filter out durations less than or equal to 5 minutes
        }.map { it / 3600f } // convert to hours

        val avg = if (filteredDurations.isEmpty()) 8f else filteredDurations.average().toFloat()
        listOf(
            0f to 0f,
            avg to 0.5f,
            16f to 1f,
        )
    }

//    Log.d("waka", "Creating calendar graph for project: $projectName with ${dateToDurationMap.size} entries")
    val weeklyData = generateWeeklyData(dateToDurationMap)

    val textColor by remember(projectColor) {
        derivedStateOf { ColorUtils.getContrastingTextColor(projectColor) }
    }

    val setDialogDayData = remember {
        { it: DayData ->
            dialogDayData = it
            showDialog = true
        }
    }

//    Log.d("waka", "The first week of data is: ${weeklyData.firstOrNull()}")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(fraction = 0.65f)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxHeight()
        ) {
            items(weeklyData) {
                var firstJanIdx: Int? = null
                it.forEachIndexed { index, it ->
                    if (it.date == 1 && it.month.uppercase() == "JANUARY") {
                        firstJanIdx = index
                        return@forEachIndexed
                    }
                }
                if (firstJanIdx != null) {
                    WeekGraph(
                        it,
                        targetInHours,
                        textColor,
                        projectColor,
                        if (firstJanIdx != 0) (0..<firstJanIdx).toSet() else emptySet(),
                        opacityStops,
                        setDialogDayData
                    )
                    // display the new year
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it.lastOrNull()?.year?.substring(0, 4) ?: "NEW YEAR",
                            fontSize = 24.sp,
                            color = projectColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (firstJanIdx != 0) {
                        WeekGraph(
                            it,
                            targetInHours,
                            textColor,
                            projectColor,
                            (firstJanIdx..7).toSet(),
                            opacityStops,
                            setDialogDayData
                        )
                    }
                } else {
                    WeekGraph(
                        it,
                        targetInHours,
                        textColor,
                        projectColor,
                        emptySet(),
                        opacityStops,
                        setDialogDayData
                    )
                }
            }
        }

        ScrollBlurEffects(lazyListState, weeklyData.size)


    }
}

// region HELPER FUNCTIONS
// ? ........................


fun calculateWindowedOpacity(durationInHours: Float, stops: List<Pair<Float, Float>>): Float {
//    val clampedDuration = (duration / 3600f).coerceIn(0f, MAX_HOURS) // Clamp to a maximum of 16 hours
//    if (clampedDuration == 0f) {
//        return 0f
//    }
//
//    val numerator = ln(clampedDuration + 1.0)
//    val denominator = ln(MAX_HOURS + 1.0)
//
//    if ((0..100).random() == 0) {
//        Log.d(
//            "waka",
//            "Calculating logarithmic opacity: duration=$duration, clampedDuration=$clampedDuration, numerator=$numerator, denominator=$denominator opacity=${numerator / denominator}"
//        )
//    }
//
////    return (numerator / denominator).toFloat().coerceIn(0f, 1f) // Ensure the result is between 0 and 1
//    return clampedDuration / 16


    // Ensure the stops are sorted by hours. This is crucial for the logic to work.
    val sortedStops = stops.sortedBy { it.first }

    // Handle edge cases where the duration is outside the defined range of stops.
    // If duration is less than or equal to the first stop's hours, return the first stop's opacity.
    if (durationInHours <= sortedStops.first().first) {
        return sortedStops.first().second
    }
    // If duration is greater than or equal to the last stop's hours, return the last stop's opacity.
    if (durationInHours >= sortedStops.last().first) {
        return sortedStops.last().second
    }

    // Find the two stops that the duration falls between.
    // `windowed(2)` creates a sliding window of two consecutive elements.
    // We find the first pair where the duration is between the hours of the two stops.
    val (startStop, endStop) = sortedStops.windowed(2).first {
        durationInHours >= it[0].first && durationInHours < it[1].first
    }

    val (startHours, startOpacity) = startStop
    val (endHours, endOpacity) = endStop

    // Perform the linear interpolation (LERP).
    // 1. Calculate the total range of hours in the current segment.
    val hourRange = endHours - startHours
    // Avoid division by zero, though this is unlikely with sorted distinct stops.
    if (hourRange == 0f) return startOpacity

    // 2. Calculate how far the duration is into this segment, as a percentage (0.0 to 1.0).
    val durationIntoSegment = durationInHours - startHours
    val progress = durationIntoSegment / hourRange

    // 3. Calculate the total range of opacity in the current segment.
    val opacityRange = endOpacity - startOpacity

    val opacity = startOpacity + (progress * opacityRange)

//    if ((0..100).random() == 0) {
//        Log.d(
//            "waka",
//            "Calculating opacity $opacity from duration $durationInHours"
//        )
//    }

    // 4. Apply the progress to the opacity range and add it to the starting opacity.
    return opacity
}

fun generateWeeklyData(
    dateToDurationMap: Map<String, Int>,
    minWeeks: Int = 8
): List<List<DayData>> {
    Log.d("waka", "Generating weekly data for project")
    // first get the earliest date and calculate the number of weeks
    val numWeeks = dateToDurationMap.let {
        if (it.isEmpty()) {
            minWeeks
        } else {
            val dates = it.keys.map { WakaHelpers.yyyyMMDDToDate(it) }
            val minDate = dates.minOrNull() ?: LocalDate.now()
            val weeks = (LocalDate.now().toEpochDay() - minDate.toEpochDay()) / 7
            maxOf(weeks.toInt(), minWeeks)
        }
    }

    val weeklyData = mutableListOf<List<DayData>>()

    // move back to monday
    val today = LocalDate.now()
    val startDate = today.minusDays((today.dayOfWeek.value - 1).toLong())


    (0..numWeeks).forEach {
        val weekData = emptyList<DayData>().toMutableList()
        val firstDateOfWeek = startDate.minusWeeks(it.toLong())
        (0..6).forEach { day ->
            // for each day of the week, generate the date string and get the duration
            val date = firstDateOfWeek.plusDays(day.toLong())
            val dateString = date.format(WakaHelpers.getYYYYMMDDDateFormatter())
            val duration = dateToDurationMap[dateString] ?: 0
            val dayData = DayData(
                date.dayOfMonth,
                date.month.toString(),
                date.dayOfWeek.toString(),
                date.year.toString(),
                dateString,
//                if (targetSeconds == 0f) {
//                    if (duration > 0) 1f else 0f
//                } else {
//                    min(1f, duration / targetSeconds)
//                },
//                min(1f, duration / (if (targetSeconds == 0f) 3600f else targetSeconds)),
//                calculateWindowedOpacity(duration, stops),
                durationInHours = duration / 3600f,
                isFutureDate = date.isAfter(today),
                isToday = date.isEqual(today),
            )
            weekData.add(dayData)
        }
        weeklyData.add(weekData)
    }

    return weeklyData;
}

fun dayOfWeekStringToIdx(dayOfWeek: String): Int {
    return when (dayOfWeek.uppercase()) {
        "MONDAY" -> 0
        "TUESDAY" -> 1
        "WEDNESDAY" -> 2
        "THURSDAY" -> 3
        "FRIDAY" -> 4
        "SATURDAY" -> 5
        "SUNDAY" -> 6
        else -> throw IllegalArgumentException("Invalid day of week: $dayOfWeek")
    }
}


// ? ........................
// endregion ........................

