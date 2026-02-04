package com.aught.wakawaka.screens.components

import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Data point for the line graph
 * @param value The numeric value to plot
 * @param label The label for this data point (e.g., "5th Apr")
 */
data class GraphDataPoint(
    val values: List<Float>,
    val color: Color,
)

/**
 * A flexible, stylized line graph component with bezier curves and glow effects.
 *
 * @param dataPoints List of data points to display
 * @param lineColor The main color for the line (will glow)
 * @param axisColor The color for the x/y axes
 * @param labelColor The color for axis labels and tickers
 * @param modifier Modifier for the component
 * @param labelInterval How often to show x-axis labels (e.g., every 5 points)
 */
@Composable
fun MultiLineGraph(
    dataPoints: List<GraphDataPoint>,
    labels: List<String>,
    axisColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    labelInterval: Int = 7
) {
    if (dataPoints.size < 2) return

    // make sure that the number of values in each data point matches the number of labels
//    if (dataPoints.any { it.values.size != labels.size }) return

    val density = LocalDensity.current
    val textSizePx = with(density) { 6.sp.toPx() }
    val tickerSize = with(density) { 4.dp.toPx() }
    val paddingLeft = with(density) { 12.dp.toPx() }
    val paddingBottom = with(density) { 12.dp.toPx() }
    val paddingTop = with(density) { 12.dp.toPx() }
    val paddingRight = with(density) { 12.dp.toPx() }

    // Calculate max value for dynamic y-axis
    val maxValue = remember(dataPoints) {
        val max = dataPoints.maxOfOrNull { it.values.max() } ?: 1f
        if (max <= 0f) 1f else max
    }

    // Calculate nice y-axis ticks
    val yTicks = remember(maxValue) {
        calculateYTicks(maxValue)
    }
    val yMax = yTicks.lastOrNull() ?: maxValue

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val graphLeft = paddingLeft
            val graphRight = canvasWidth - paddingRight
            val graphTop = paddingTop
            val graphBottom = canvasHeight - paddingBottom
            val graphWidth = graphRight - graphLeft
            val graphHeight = graphBottom - graphTop

            val axisColorArgb = axisColor.toArgb()
            val labelColorArgb = labelColor.toArgb()


            // region Y AXIS
            // ? ........................

            // Draw Y-axis
            drawLine(
                color = axisColor,
                start = Offset(graphLeft, graphTop),
                end = Offset(graphLeft, graphBottom),
                strokeWidth = 2f
            )
            // Draw Y-axis ticks and labels
            val textPaint = Paint().apply {
                color = labelColorArgb
                textSize = textSizePx
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            yTicks.forEach { tickValue ->
                val yPos = graphBottom - (tickValue / yMax) * graphHeight

                // Tick mark
                drawLine(
                    color = axisColor,
                    start = Offset(graphLeft - tickerSize, yPos),
                    end = Offset(graphLeft, yPos),
                    strokeWidth = 4f
                )

                // Label
                drawContext.canvas.nativeCanvas.drawText(
                    tickValue.toInt().toString(),
                    graphLeft - tickerSize - 8f,
                    yPos + textSizePx / 3,
                    textPaint
                )
            }

            // ? ........................
            // endregion ........................

            val xStep = graphWidth / (labels.size - 1)


            // draw each set of data points
            dataPoints.forEachIndexed { i,it ->
                val path = Path()

                val points: MutableList<Offset> = mutableListOf()

                it.values.forEachIndexed { index, point ->
                    // if point is NaN, break
                    if (point == -1f) return@forEachIndexed
                    val x = graphLeft + index * xStep
                    val y = graphBottom - (point / yMax) * graphHeight
                    points.add(Offset(x, y))
                }
                Log.d("MultiLineGraph", "Data points for line $i: $points from ${it.values}")

                path.moveTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]

                    // Control points for smooth bezier curve
                    val controlPointFactor = 0.5f
                    val dx = (p1.x - p0.x) * controlPointFactor

                    val cp1 = Offset(p0.x + dx, p0.y)
                    val cp2 = Offset(p1.x - dx, p1.y)

                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                }

//                // Draw glow effect (multiple passes with decreasing alpha)
//                val glowColor = it.color.copy(alpha = 0.15f)
//                listOf(12f, 8f, 5f).forEach { strokeWidth ->
//                    drawPath(
//                        path = path,
//                        color = glowColor,
//                        style = Stroke(
//                            width = strokeWidth,
//                            cap = StrokeCap.Round,
//                            join = StrokeJoin.Round
//                        )
//                    )
//                }

                // Draw main line
                drawPath(
                    path = path,
                    color = it.color,
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw data points
                if (i == 0) {
                    points.forEach { point ->
                        // Glow behind point
//                        drawCircle(
//                            color = lineColor.copy(alpha = 0.3f),
//                            radius = 6f,
//                            center = Offset(point.x, graphBottom)
//                        )
                        // Point itself
                        drawCircle(
                            color = axisColor,
                            radius = 3f,
                            center = Offset(point.x, graphBottom)
                        )
                    }
                }
            }
            // draw x axis after lines so it appears on top

            // region X AXIS
            // ? ........................

            // Draw X-axis
            drawLine(
                color = axisColor,
                start = Offset(graphLeft, graphBottom),
                end = Offset(graphRight, graphBottom),
                strokeWidth = 2f
            )

            // Draw X-axis labels
            val xTextPaint = Paint().apply {
                color = labelColorArgb
                textSize = textSizePx * 0.9f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            labels.forEachIndexed { index, label ->
                val xPos = graphLeft + index * xStep

                // Show label at intervals and always show first and last
                if (index % labelInterval == 0
//                    || index == labels.size - 1
                    ) {
                    // Tick mark
                    drawLine(
                        color = axisColor,
                        start = Offset(xPos, graphBottom),
                        end = Offset(xPos, graphBottom + tickerSize),
                        strokeWidth = 4f
                    )

                    // Label
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        xPos,
                        graphBottom + tickerSize + textSizePx + 4f,
                        xTextPaint
                    )
                }
            }
            // ? ........................
            // endregion ........................
        }
    }
}

/**
 * Calculate nice tick values for y-axis
 */
private fun calculateYTicks(maxValue: Float): List<Float> {
    if (maxValue <= 0) return listOf(0f, 1f)

    // Find a nice step size
    val roughStep = maxValue / 2
    val magnitude = floor(log10(roughStep.toDouble())).toInt()
    val power = 10.0.pow(magnitude.toDouble()).toFloat()

    val niceStep = when {
        roughStep / power < 1.5 -> power
        roughStep / power < 3 -> 2 * power
        roughStep / power < 7 -> 5 * power
        else -> 10 * power
    }

    val ticks = mutableListOf<Float>()
    var tick = 0f
    while (tick <= maxValue * 1.1f) {
        ticks.add(tick)
        tick += niceStep
    }

    // Ensure we have at least the max value covered
    if (ticks.last() < maxValue) {
        ticks.add(ticks.last() + niceStep)
    }

    return ticks
}
