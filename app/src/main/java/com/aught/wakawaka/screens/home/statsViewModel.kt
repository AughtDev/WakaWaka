package com.aught.wakawaka.screens.home

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.WakaDataTransformers
import com.aught.wakawaka.data.WakaDataUseCase
import com.aught.wakawaka.screens.components.GraphDataPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields


data class DateData(
    val yyyyMmDd: String,
    val durationMap: List<Pair<String, Long>>, // project to duration in seconds
)

data class OffsetOption(
    val offset: Int,
    val label: String
)

data class StatsUiState(
    val dataLoading: Boolean = false,
    val graphState: GraphState = GraphState.MONTHLY,
    val graphDataPoints: List<GraphDataPoint> = emptyList(),
    val projectSummaryData: List<SummaryData> = emptyList(),
    val graphYLabels: List<String> = emptyList(),
    val graphXLabelInterval: Int = 1,
    val graphOffset: Int = 0,
    val graphLabel: String = "",
    val availableOffsets: List<OffsetOption> = emptyList(),
)

/**
 * Generate list of available offsets with their labels based on earliest date in data
 */
fun generateAvailableOffsets(
    allData: List<DateData>,
    state: GraphState
): List<OffsetOption> {
    if (allData.isEmpty()) return emptyList()

    // Find earliest date in all data
    val earliestDate = allData.minOf { LocalDate.parse(it.yyyyMmDd) }
    val now = LocalDate.now()

    val offsets = mutableListOf<OffsetOption>()
    var currentOffset = 0

    while (true) {
        // Calculate the date for this offset
        val targetDate = when (state) {
            GraphState.WEEKLY -> now.minusWeeks(currentOffset.toLong())
            GraphState.MONTHLY -> now.minusMonths(currentOffset.toLong())
            GraphState.QUARTERLY -> now.minusMonths((currentOffset * 3).toLong())
            GraphState.YEARLY -> now.minusYears(currentOffset.toLong())
        }

        // Check if we've gone past the earliest data
        val startOfPeriod = when (state) {
            GraphState.WEEKLY -> targetDate.with(DayOfWeek.MONDAY)
            GraphState.MONTHLY -> targetDate.withDayOfMonth(1)
            GraphState.QUARTERLY -> {
                val quarterStartMonth = ((targetDate.monthValue - 1) / 3) * 3 + 1
                targetDate.withMonth(quarterStartMonth).withDayOfMonth(1)
            }
            GraphState.YEARLY -> targetDate.withDayOfYear(1)
        }

        // Stop if the start of this period is before the earliest data
        if (startOfPeriod.isBefore(earliestDate)) {
            break
        }

        // Generate label and add to list
        val label = offsetToLabel(state, currentOffset)
        offsets.add(OffsetOption(currentOffset, label))

        currentOffset++
    }

    return offsets
}

// Batch size mapping: how many days to aggregate per data point
val GRAPH_BATCH_SIZES = mapOf(
    GraphState.WEEKLY to 1,      // 7 days -> 7 points
    GraphState.MONTHLY to 2,     // 30 days -> 15 points
    GraphState.QUARTERLY to 5,   // 90 days -> 18 points
    GraphState.YEARLY to 14       // 365 days -> 26 points
)

// Label interval mapping: show ~5 labels on x-axis
val GRAPH_LABEL_INTERVALS = mapOf(
    GraphState.WEEKLY to 1,      // 7 points / 1 = 7 labels (small enough)
    GraphState.MONTHLY to 3,     // 15 points / 3 = 5 labels
    GraphState.QUARTERLY to 4,   // 18 points / 4 = 5 labels
    GraphState.YEARLY to 5      // 26 points / 5 = 5-6 labels
)

// region GRAPH DATA GENERATION
// ? ........................

fun generateGraphData(projectData: List<ProjectSpecificData>): List<DateData> {
    val graphData: MutableMap<String, MutableList<Pair<String, Long>>> = mutableMapOf()
    for (project in projectData) {
        for ((date, duration) in project.dailyDurationInSeconds) {
            if (duration <= 0) continue
            val entry = graphData.getOrPut(date) { mutableListOf() }
            entry.add(Pair(project.name, duration.toLong()))
        }
    }
    return graphData.map { (date, durationMap) ->
        DateData(
            yyyyMmDd = date,
            durationMap = durationMap
        )
    }.sortedBy { it.yyyyMmDd }
}


fun offsetToLabel(state: GraphState, offset: Int): String {
    // week is of the form Week 6, 2026,
    // month is of the form January 2026,
    // quarter is of the form Q1 2026,
    // year is of the form 2026

    val now = LocalDate.now()
    return when (state) {
        GraphState.WEEKLY -> {
            val targetDate = now.minusWeeks(offset.toLong())
            val weekOfYear = targetDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            "Week $weekOfYear, ${targetDate.year}"
        }

        GraphState.MONTHLY -> {
            val targetDate = now.minusMonths(offset.toLong())
            "${
                targetDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
            } ${targetDate.year}"
        }

        GraphState.QUARTERLY -> {
            val targetDate = now.minusMonths((offset * 3).toLong())
            val quarter = (targetDate.monthValue - 1) / 3 + 1
            "Q$quarter ${targetDate.year}"
        }

        GraphState.YEARLY -> {
            val targetDate = now.minusYears(offset.toLong())
            "${targetDate.year}"
        }
    }
}

fun sliceDateData(data: List<DateData>, state: GraphState, offset: Int): List<DateData> {
    val now = LocalDate.now()
    val startDate: LocalDate = when (state) {
        GraphState.WEEKLY -> now.minusWeeks(offset.toLong()).with(DayOfWeek.MONDAY)
        GraphState.MONTHLY -> now.minusMonths(offset.toLong()).withDayOfMonth(1)
        GraphState.QUARTERLY -> {
            val targetDate = now.minusMonths((offset * 3).toLong())
            val quarterStartMonth = ((targetDate.monthValue - 1) / 3) * 3 + 1
            targetDate.withMonth(quarterStartMonth).withDayOfMonth(1)
        }

        GraphState.YEARLY -> now.minusYears(offset.toLong()).withDayOfYear(1)
    }
    val endDate: LocalDate = when (state) {
        GraphState.WEEKLY -> startDate.plusWeeks(1)
        GraphState.MONTHLY -> startDate.plusMonths(1)
        GraphState.QUARTERLY -> startDate.plusMonths(3)
        GraphState.YEARLY -> startDate.plusYears(1)
    }

    Log.d("StatsViewModel", "Slicing data from $startDate to $endDate")

    val rawData = data.filter { point ->
        val pointDate = LocalDate.parse(point.yyyyMmDd)
        !pointDate.isBefore(startDate) && pointDate.isBefore(endDate)
    }
    // pad the raw data making sure that every date in the range is represented even if the duration map is empty
    val currDates: Set<String> = rawData.map { it.yyyyMmDd }.toSet()
    val paddedData = mutableListOf<DateData>()
    var currentDate = startDate
    while (currentDate.isBefore(endDate)) {
        val dateStr = currentDate.toString()
        if (currDates.contains(dateStr)) {
            val existingPoint = rawData.first { it.yyyyMmDd == dateStr }
            paddedData.add(existingPoint)
        } else {
            paddedData.add(
                DateData(
                    yyyyMmDd = dateStr,
                    durationMap = emptyList()
                )
            )
        }
        currentDate = currentDate.plusDays(1)

    }

    // now convert to GraphDataPoints, that means that for each project where the aggregate duration over the period is > 0, we create a GraphDataPoint
    return paddedData
}

data class SummaryData(
    val color: Color,
    val durationS: Long,
    val projectName: String,
    val humanReadableDuration: String
)

/**
 * Batches DateData by aggregating durations over specified number of days
 * Returns batched data where each batch contains summed durations for that period
 */
fun batchDateData(data: List<DateData>, batchSize: Int): List<DateData> {
    if (batchSize <= 1 || data.isEmpty()) return data

    val batchedData = mutableListOf<DateData>()
    var currentBatch = mutableListOf<DateData>()

    for (point in data) {
        currentBatch.add(point)

        if (currentBatch.size >= batchSize) {
            // Aggregate this batch
            val aggregatedDurationMap = mutableMapOf<String, Long>()
            for (batchPoint in currentBatch) {
                for ((project, duration) in batchPoint.durationMap) {
                    aggregatedDurationMap[project] = (aggregatedDurationMap[project] ?: 0L) + duration
                }
            }

            // Use the last date in the batch as the representative date
            val representativeDate = currentBatch.last().yyyyMmDd
            batchedData.add(
                DateData(
                    yyyyMmDd = representativeDate,
                    durationMap = aggregatedDurationMap.toList()
                )
            )

            currentBatch = mutableListOf()
        }
    }

    // Handle any remaining data in the last incomplete batch
    if (currentBatch.isNotEmpty()) {
        val aggregatedDurationMap = mutableMapOf<String, Long>()
        for (batchPoint in currentBatch) {
            for ((project, duration) in batchPoint.durationMap) {
                aggregatedDurationMap[project] = (aggregatedDurationMap[project] ?: 0L) + duration
            }
        }

        val representativeDate = currentBatch.last().yyyyMmDd
        batchedData.add(
            DateData(
                yyyyMmDd = representativeDate,
                durationMap = aggregatedDurationMap.toList()
            )
        )
    }

    return batchedData
}

fun generateLabels(
    data: List<DateData>,
): List<String> {
    return data.map { point ->
        val date = LocalDate.parse(point.yyyyMmDd)
        // the date in the form 5th Jan
        "${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }.substring(0, 3)}"
    }
}

fun generateGraphDataPoints(
    data: List<DateData>,
    colorMap: Map<String, Color>
): List<GraphDataPoint> {
    val projectDurationMap = mutableMapOf<String, MutableList<Long>>()
    // first add all available projects to the map
    for (point in data) {
        for ((project, _) in point.durationMap) {
            projectDurationMap.putIfAbsent(project, mutableListOf())
        }
    }
    val nowYyyMmDd = LocalDate.now().toString()

    Log.d("StatsViewModel", "nowYyyMmDd: $nowYyyMmDd")

    for (point in data) {
        val durationMap = point.durationMap.toMap()
        for ((project, duration) in durationMap) {
            val durations = projectDurationMap.getOrPut(project) { mutableListOf() }
            durations.add(duration)
        }
        // ensure that projects with no duration on this date get a 0 entry
        for (project in projectDurationMap.keys) {
            if (!durationMap.containsKey(project)) {
                val durations = projectDurationMap[project]!!
                // if the date is past the last date in data, we do not add a 0
                if (point.yyyyMmDd <= nowYyyMmDd) {
                    durations.add(0L)
                } else {
//                    Log.d("StatsViewModel", "Adding NaN for project $project on future date ${point.yyyyMmDd}")
//                    // if future date, add NaN to indicate no data
//                    durations.add(Long.MAX_VALUE)
                }
            }
        }
    }
    return projectDurationMap.map { (project, durations) ->
        GraphDataPoint(
            values = durations.map { it / 3600f },
            color = colorMap[project] ?: Color.Gray,
        )
    }
}

fun durationInSecondsToHumanReadable(durationInSeconds: Long): String {
    val hours = durationInSeconds / 3600
    val minutes = (durationInSeconds % 3600) / 60
//    val seconds = durationInSeconds % 60
    val parts = mutableListOf<String>()
    if (hours > 0) {
        parts.add("$hours h")
    }
    if (minutes > 0) {
        parts.add("$minutes m")
    }
//    if (seconds > 0 || parts.isEmpty()) {
//        parts.add("$seconds s")
//    }
    return parts.joinToString(" ")
}

fun generateSummaryData(
    data: List<DateData>,
    colorMap: Map<String, Color>
): List<SummaryData> {
    val durationMap = mutableMapOf<String, Long>()
    for (point in data) {
        for ((project, duration) in point.durationMap) {
            durationMap[project] = (durationMap[project] ?: 0L) + duration
        }
    }
    val summaryData = durationMap.map { (project, duration) ->
        SummaryData(
            color = colorMap[project] ?: Color.Gray,
            projectName = project,
            durationS = duration,
            humanReadableDuration = durationInSecondsToHumanReadable(duration)
        )
    }.sortedByDescending { it.durationS }
    return summaryData
}

fun deriveColorMap(projectData: List<ProjectSpecificData>): Map<String, Color> {
    val colorMap = mutableMapOf<String, Color>()
    for (project in projectData) {
        colorMap[project.name] = WakaDataTransformers.getProjectColor(project) ?: Color.Gray
    }
    return colorMap
}

// ? ........................
// endregion ........................


enum class GraphState {
    WEEKLY, MONTHLY, QUARTERLY, YEARLY
}

class StatsViewModel(
    private val wakaDataUseCase: WakaDataUseCase
) : ViewModel() {

    // region GRAPH STATE
    // ? ........................

    private val _graphState = MutableStateFlow(GraphState.MONTHLY)
    val graphState: StateFlow<GraphState> = _graphState

    fun setGraphState(state: GraphState) {
        _graphState.value = state
        // update the offset to 0 when changing state
        _graphOffset.value = 0
    }

    // ? ........................
    // endregion ........................

    // region GRAPH OFFSET
    // ? ........................

    private val _graphOffset = MutableStateFlow(0)
    val graphOffset: StateFlow<Int> = _graphOffset

    fun moveForward() {
        _graphOffset.value = (_graphOffset.value - 1).coerceAtLeast(0)
    }

    fun moveBackward() {
        _graphOffset.value = _graphOffset.value + 1
    }

    fun setOffset(offset: Int) {
        _graphOffset.value = offset.coerceAtLeast(0)
    }

    // ? ........................
    // endregion ........................

    val colorMap: StateFlow<Map<String, Color>> = wakaDataUseCase.getProjects().map {
        deriveColorMap(it)
    }.stateIn(
        viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    val graphData: StateFlow<List<DateData>> = wakaDataUseCase.getProjects().map {
        generateGraphData(it)
    }.stateIn(
        viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<StatsUiState> = combine(
        colorMap,
        graphData,
        graphState,
        graphOffset,
    ) { colorMap, data, state, offset ->
        val slicedData = sliceDateData(data, state, offset)

        // Apply batching based on graph state
        val batchSize = GRAPH_BATCH_SIZES[state] ?: 1
        val batchedData = batchDateData(slicedData, batchSize)

        val dataPoints = generateGraphDataPoints(batchedData, colorMap)
        val summaryData = generateSummaryData(slicedData, colorMap)
        val labels = generateLabels(batchedData)
        val labelInterval = GRAPH_LABEL_INTERVALS[state] ?: 1

        // Generate available offsets based on entire data range
        val availableOffsets = generateAvailableOffsets(data, state)

        val label = offsetToLabel(state, offset)
        StatsUiState(
            dataLoading = false,
            graphState = state,
            graphDataPoints = dataPoints,
            projectSummaryData = summaryData,
            graphYLabels = labels,
            graphXLabelInterval = labelInterval,
            graphOffset = offset,
            graphLabel = label,
            availableOffsets = availableOffsets,
        )
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        StatsUiState(dataLoading = true)
    )
}


//great, now another task. i’ve done much of the data work and set up. theres a @app/src/main/java/com/aught/wakawaka/screens/home/stats.kt file with a stats button that opens up a stats dialog. then theres a @app/src/main/java/com/aught/wakawaka/screens/home/statsViewModel.kt to collect the data from the repo and a @app/src/main/java/com/aught/wakawaka/screens/components/line-graph.kt component for multiline graphs. i want to be able to see my coding stats as a multi line graph. this dialog is meant to have, a header with at the top left, the label of the curent data, on the top right a tag showing the timeperiod that on click opens a selection dropdown. below the header is the multiline graph showing the appropriate data. below that is a lazy list showing panes that act as a key. each pane is, on the far left, a circle tag with the color of the project, besides that the project name, to the far right, the human reaadable duration within the specified offset and time period. the panes are sorted by duration. swiping left and right on the graph updates the offset to view previous and subsequent time periods. Plan this out and ask questions if necessary
