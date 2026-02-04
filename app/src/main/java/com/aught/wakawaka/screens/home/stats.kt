package com.aught.wakawaka.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.aught.wakawaka.screens.home.OffsetOption
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.aught.wakawaka.screens.components.MultiLineGraph
import org.koin.androidx.compose.koinViewModel
import scrollBlurEffects

@Composable
fun StatsButton(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = koinViewModel()
) {
    var statsDialogOpen by remember { mutableStateOf(false) }

    IconButton(
        onClick = { statsDialogOpen = true },
        modifier = modifier,
    ) {
        Icon(
            Icons.Default.BarChart,
            contentDescription = "Stats"
        )
    }

    if (statsDialogOpen) {
        Dialog(
            onDismissRequest = { statsDialogOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            StatsDialogContent(
                viewModel = viewModel,
                close = { statsDialogOpen = false }
            )
        }
    }
}

@Composable
fun StatsDialogContent(
    viewModel: StatsViewModel,
    close: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .background(
                MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.8f),
                RoundedCornerShape(12.dp)
            ).padding(16.dp)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header Row
            StatsHeader(
                label = uiState.graphLabel,
                currentState = uiState.graphState,
                currentOffset = uiState.graphOffset,
                availableOffsets = uiState.availableOffsets,
                onStateChange = { viewModel.setGraphState(it) },
                onOffsetChange = { viewModel.setOffset(it) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Graph with swipe
            if (uiState.graphDataPoints.isNotEmpty()) {
                SwipeableGraph(
                    dataPoints = uiState.graphDataPoints,
                    labels = uiState.graphYLabels,
                    onSwipeLeft = { viewModel.moveForward() },
                    onSwipeRight = { viewModel.moveBackward() },
                    labelInterval = uiState.graphXLabelInterval,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                NoDataView()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key list (sorted by duration)
            val lazyListState = rememberLazyListState()
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollBlurEffects(lazyListState, uiState.projectSummaryData.size),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.projectSummaryData) { summary ->
                    SummaryPane(
                        color = summary.color,
                        projectName = summary.projectName,
                        duration = summary.humanReadableDuration
                    )
                }
            }
        }
        val totalDurationS = uiState.projectSummaryData.sumOf { it.durationS }
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = durationInSecondsToHumanReadable(totalDurationS),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
fun StatsHeader(
    label: String,
    currentState: GraphState,
    currentOffset: Int,
    availableOffsets: List<OffsetOption>,
    onStateChange: (GraphState) -> Unit,
    onOffsetChange: (Int) -> Unit
) {
    var periodDropdownExpanded by remember { mutableStateOf(false) }
    var offsetDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on left - now clickable with CURRENT tag
        Box(
            modifier = Modifier.clickable { offsetDropdownExpanded = true }
        ) {
            Column(
                modifier = Modifier.offset(y=(8).dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currentOffset > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            ).padding(2.dp)
                    ) {
                        // CURRENT tag - only visible at offset 0
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Offset dropdown
            if (offsetDropdownExpanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = { offsetDropdownExpanded = false },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    OffsetDropdownMenuContent(
                        availableOffsets = availableOffsets,
                        currentOffset = currentOffset,
                        onOffsetSelected = {
                            onOffsetChange(it)
                            offsetDropdownExpanded = false
                        },
                        onDismiss = { offsetDropdownExpanded = false }
                    )
                }
            }
        }

        // Time period tag with dropdown - wrapped in Box for overlay
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Box {
                TimePeriodTag(
                    state = currentState,
                    onClick = { periodDropdownExpanded = !periodDropdownExpanded }
                )

                // Dropdown menu overlaid using Popup for true overlay
                if (periodDropdownExpanded) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        onDismissRequest = { periodDropdownExpanded = false },
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        DropdownMenuContent(
                            currentState = currentState,
                            onStateSelected = {
                                onStateChange(it)
                                periodDropdownExpanded = false
                            },
                            onDismiss = { periodDropdownExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

data class TimeTagData(
    val label: String,
    val color: Color
)

fun periodToTagData(state: GraphState): TimeTagData {
    return when (state) {
        GraphState.WEEKLY -> TimeTagData("Weekly", Color(0xFF4CAF50))
        GraphState.MONTHLY -> TimeTagData("Monthly", Color(0xFF2196F3))
        GraphState.QUARTERLY -> TimeTagData("Quarterly", Color(0xFFFF9800))
        GraphState.YEARLY -> TimeTagData("Yearly", Color(0xFFF44336))
    }
}

@Composable
fun TimePeriodTag(
    state: GraphState,
    onClick: () -> Unit
) {
    val tagData = periodToTagData(state)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(tagData.color.copy(0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = "Select period",
            modifier = Modifier.size(6.dp),
            tint = tagData.color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = tagData.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = tagData.color
        )
    }
}

@Composable
fun OffsetDropdownMenuContent(
    availableOffsets: List<OffsetOption>,
    currentOffset: Int,
    onOffsetSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Find index of current offset to scroll to
    val currentIndex = availableOffsets.indexOfFirst { it.offset == currentOffset }.coerceAtLeast(0)
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0))
    
    Box(
        modifier = Modifier
            .padding(top = 40.dp)
            .heightIn(max = 200.dp)
            .width(170.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(availableOffsets) { option ->
                val isSelected = option.offset == currentOffset

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(0.1f)
                            else
                                Color.Transparent
                        )
                        .clickable {
                            onOffsetSelected(option.offset)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownMenuContent(
    currentState: GraphState,
    onStateSelected: (GraphState) -> Unit,
    onDismiss: () -> Unit
) {
    val options = GraphState.entries

    Box(
        modifier = Modifier
            .padding(top = 40.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            options.forEach { state ->
                val isSelected = state == currentState
                val data = periodToTagData(state)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(0.1f)
                            else
                                Color.Transparent
                        )
                        .clickable {
                            onStateSelected(state)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = data.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected)
                            data.color
                        else
                            data.color.copy(0.7f)
                    )
                }
            }
        }
    }
}

@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun SwipeableGraph(
    dataPoints: List<com.aught.wakawaka.screens.components.GraphDataPoint>,
    labels: List<String>,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    labelInterval: Int,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember { mutableStateOf(0f) }
    var showSwipeIndicator by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }

    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(200),
        label = "swipeOffset"
    )

    Box(modifier = modifier) {
        // Main graph
        MultiLineGraph(
            dataPoints = dataPoints,
            labels = labels,
            axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            labelInterval = labelInterval,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = animatedOffset
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { showSwipeIndicator = true },
                        onDragEnd = {
                            when {
                                swipeOffset > 100f -> {
                                    onSwipeRight()
                                }

                                swipeOffset < -100f -> {
                                    onSwipeLeft()
                                }
                            }
                            swipeOffset = 0f
                            showSwipeIndicator = false
                            swipeDirection = SwipeDirection.NONE
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffset += dragAmount
                            swipeDirection = when {
                                dragAmount > 0 -> SwipeDirection.RIGHT
                                dragAmount < 0 -> SwipeDirection.LEFT
                                else -> SwipeDirection.NONE
                            }
                        }
                    )
                }
        )

        // Swipe indicators
        if (showSwipeIndicator && swipeOffset > 50f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .alpha((swipeOffset / 100f).coerceIn(0f, 1f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Swipe right",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (showSwipeIndicator && swipeOffset < -50f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .alpha((-swipeOffset / 100f).coerceIn(0f, 1f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Swipe left",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private enum class SwipeDirection {
    LEFT, RIGHT, NONE
}

@Composable
fun SummaryPane(
    color: Color,
    projectName: String,
    duration: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Color circle + project name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )

            // Project name
            Text(
                text = projectName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Duration
        Text(
            text = duration,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        )
    }
}

@Composable
fun NoDataView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "No data for this period",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
