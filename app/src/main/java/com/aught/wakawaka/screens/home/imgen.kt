package com.aught.wakawaka.screens.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Size
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import okio.IOException
import java.io.File
import java.io.FileOutputStream


// region HELPERS
// ? ........................

fun linearInterpolation(v: Float, stops: List<Pair<Float, Float>>): Float {
    val stops = stops.sortedBy { it.first }

    if (v <= stops.first().first) return stops.first().second
    if (v >= stops.last().first) return stops.last().second

    var lowerStop = stops[0]
    var upperStop = stops[stops.size - 1]
    for (i in 1 until stops.size) {
        if (v < stops[i].first) {
            upperStop = stops[i]
            lowerStop = stops[i - 1]
            break
        }
    }
    val range = upperStop.first - lowerStop.first
    val rangePct = (v - lowerStop.first) / range
    return lowerStop.second + rangePct * (upperStop.second - lowerStop.second)
}

// ? ........................
// endregion ........................


fun generateImage(
    imageName: String,
    imageSize: Size,
    context: Context,
    drawToCanvas: Canvas.() -> Unit
): Uri? {
    val bitmap = createBitmap(imageSize.width, imageSize.height)
    val canvas = Canvas(bitmap)

    canvas.apply {
        drawToCanvas()
    }

    val imagesDir = File(context.cacheDir, "images")
    if (!imagesDir.exists()) imagesDir.mkdirs()
    val file = File(imagesDir, "$imageName.png")
    try {
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

enum class Coord {
    X, Y
}

data class ImageColors(
    val background: Color,
    val foreground: Color,
    val primary: Color,
    val secondary: Color,
)

fun generateCalendarShareImage(context: Context, projectName: String?, imageColors: ImageColors, dateToDurationInSeconds: Map<String, Int>): Uri? {
    val CELL_SIZE = 20f
    val CELL_MARGIN = 3f
    val CELL_RADIUS = 4f

    val HEADER_HEIGHT = 50f
    val YEAR_LABEL_HEIGHT = 30f
    val YEAR_LABEL_PADDING = 10f

    val PADDING = 20f

    val MIN_NUM_YEARS = 4

    val numYears = dateToDurationInSeconds.keys.map {
        it.split("-")[0].toIntOrNull() ?: 0
    }.distinct().size.coerceAtLeast(MIN_NUM_YEARS)

    val imageWidth = (PADDING * 2 + (CELL_SIZE + CELL_MARGIN) * 53 - CELL_MARGIN).toInt()
    val imageHeight = (
            PADDING * 2 + HEADER_HEIGHT +
                    (YEAR_LABEL_HEIGHT + YEAR_LABEL_PADDING * 2) * numYears +
                    ((CELL_SIZE + CELL_MARGIN) * 7 - CELL_MARGIN) * numYears
            ).toInt()

    val innerImgWidth = imageWidth - PADDING * 2
    val innerImgHeight = imageHeight - PADDING * 2

    val yearCalendarHeight = (CELL_SIZE + CELL_MARGIN) * 7 - CELL_MARGIN
    val yearHeaderHeight = YEAR_LABEL_HEIGHT + YEAR_LABEL_PADDING * 2
    val yearBlockHeight = yearHeaderHeight + yearCalendarHeight


    val opacityStops = {
        // only consider durations above 5 minutes
        val filteredDurations = dateToDurationInSeconds.values.filter {
            it > 60 * 5 // filter out durations less than or equal to 5 minutes
        }.map { it / 3600f } // convert to hours

        val avg = if (filteredDurations.isEmpty()) 8f else filteredDurations.average().toFloat()
        listOf(
            0f to 0f,
            avg to 0.5f,
            16f to 1f,
        )
    }()

    fun toImage(v: Number, coord: Coord = Coord.Y): Float {
        return when (coord) {
            Coord.X -> (PADDING + v.toFloat())
            Coord.Y -> (PADDING + v.toFloat())
        }
    }

    fun toYearHeader(v: Number, yrIdx: Int, coord: Coord = Coord.Y): Float {
        return toImage(
            when (coord) {
                Coord.X -> v
                Coord.Y -> HEADER_HEIGHT + (yearBlockHeight * yrIdx) + v.toFloat()
            }, coord
        )
    }

    fun toYearWeek(v: Number, yrIdx: Int, weekIdx: Int, coord: Coord = Coord.Y): Float {
        return toImage(
            when (coord) {
                Coord.X -> v.toFloat() + (CELL_SIZE + CELL_MARGIN) * weekIdx
                Coord.Y -> HEADER_HEIGHT + yearBlockHeight * yrIdx + yearHeaderHeight + v.toFloat()
            }, coord
        )
    }

    fun toYearCell(v: Number, yrIdx: Int, weekIdx: Int, dayIdx: Int, coord: Coord = Coord.Y): Float {
        return toYearWeek(
            when (coord) {
                Coord.X -> v
                Coord.Y -> v.toFloat() + (CELL_SIZE + CELL_MARGIN) * dayIdx
            }, yrIdx, weekIdx, coord
        )
    }

    val years: List<String> = dateToDurationInSeconds.keys.map {
        it.split("-")[0]
    }.distinct().sortedDescending()

    val totalHours = dateToDurationInSeconds.values.sum() / 3600f


    return generateImage(
        "calendar_share_image",
        Size(800, 200),
        context
    ) {
        // fill background
        drawRect(
            0f, 0f, imageWidth.toFloat(), imageHeight.toFloat(),
            Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                color = imageColors.background.toArgb()
            }
        )

        // region HEADER
        // ? ........................

        // on the left, the project name (or "Aggregate" if null)
        drawText(
            projectName ?: "Aggregate",
            0f, toImage(0, Coord.Y) + 0f,
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = imageColors.foreground.toArgb()
                textSize = 40f
                isFakeBoldText = true
                textAlign = Paint.Align.LEFT
            }
        )

        // on the right, the total hours
        drawText(
            String.format("%.1f hrs", totalHours),
            imageWidth - PADDING, toImage(0, Coord.Y) + 0f,
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = imageColors.foreground.toArgb()
                textSize = 40f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }
        )

        // ? ........................
        // endregion ........................

        // region YEARS
        // ? ........................

        // for each year, iterate through each day and draw a cell
        for (yrIdx in years.indices) {
            val year = years[yrIdx]
            // draw year label
            drawText(
                year,
                toYearHeader(innerImgWidth / 2, yrIdx, Coord.X),
                toYearHeader(yearHeaderHeight / 2, yrIdx, Coord.Y),
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = imageColors.foreground.toArgb()
                    textSize = YEAR_LABEL_HEIGHT.toFloat()
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
            )

            // get the first day of the year then move back to the Monday of said week
            val firstDayOfYear = java.time.LocalDate.of(year.toInt(), 1, 1)
            var currentDay = firstDayOfYear.minusDays(firstDayOfYear.dayOfWeek.value.toLong() - 1)

            // iterate through the weeks (in batches of 7 days) until we reach the next year
            var weekIdx = 0
            while (currentDay.year.toString() <= year) {
                // for each day of the week
                for (dayIdx in 0 until 7) {
                    val dateStr = currentDay.toString() // "YYYY-MM-DD"
                    val durationInSeconds = dateToDurationInSeconds[dateStr] ?: 0
                    val durationInHours = durationInSeconds / 3600f

                    // check if the date is within the current year
                    if (currentDay.year.toString() != year) break

                    // determine opacity based on duration
                    val opacity = linearInterpolation(durationInHours, opacityStops).coerceIn(0f, 1f)

                    // draw cell
                    drawRoundRect(
                        RectF(
                            toYearCell(0, yrIdx, weekIdx, dayIdx, Coord.X),
                            toYearCell(0, yrIdx, weekIdx, dayIdx, Coord.Y),
                            toYearCell(CELL_SIZE, yrIdx, weekIdx, dayIdx, Coord.X),
                            toYearCell(CELL_SIZE, yrIdx, weekIdx, dayIdx, Coord.Y),
                        ), CELL_RADIUS, CELL_RADIUS,
                        Paint().apply {
                            style = Paint.Style.FILL
                            isAntiAlias = true
                            color = imageColors.primary.copy(alpha = opacity).toArgb()
                        }
                    )

                    currentDay = currentDay.plusDays(1)
                }
                weekIdx++
            }
        }


        // ? ........................
        // endregion ........................


    }
}

fun generateDailyShareImage(context: Context, dateToDurationInSeconds: Map<String, Int>): Uri? {
    return generateImage(
        "daily_share_image",
        Size(800, 400),
        context
    ) { }
}
