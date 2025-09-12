package com.aught.wakawaka.screens.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.aught.wakawaka.R
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate


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

fun generateCalendarShareImage(
    context: Context,
    projectName: String?,
    imageColors: ImageColors,
    dateToDurationInSeconds: Map<String, Int>
): Uri? {
    val CELL_SIZE = 40f
    val CELL_MARGIN = 5f
    val CELL_RADIUS = 8f

    val HEADER_HEIGHT = 70f
    val HEADER_PADDING = 10f

    val YEAR_LABEL_HEIGHT = 50f
    val YEAR_LABEL_PADDING = 5f

    val PADDING = 30f

    val MIN_NUM_YEARS = 4

    val numYears = dateToDurationInSeconds.keys.map {
        it.split("-")[0].toIntOrNull() ?: 0
    }.distinct().size.coerceAtLeast(MIN_NUM_YEARS)

    val imageWidth = (PADDING * 2 + (CELL_SIZE + CELL_MARGIN) * 53 - CELL_MARGIN).toInt()
    val imageHeight = (
            PADDING * 2 + HEADER_HEIGHT + HEADER_PADDING * 2 +
                    (YEAR_LABEL_HEIGHT + YEAR_LABEL_PADDING * 2) * numYears +
                    ((CELL_SIZE + CELL_MARGIN) * 7 - CELL_MARGIN) * numYears
            ).toInt()

    val innerImgWidth = imageWidth - PADDING * 2
    val innerImgHeight = imageHeight - PADDING * 2

    val yearCalendarHeight = (CELL_SIZE + CELL_MARGIN) * 7 - CELL_MARGIN
    val yearHeaderHeight = YEAR_LABEL_HEIGHT + YEAR_LABEL_PADDING * 2
    val yearBlockHeight = yearHeaderHeight + yearCalendarHeight
    val fullHeaderHeight = HEADER_HEIGHT + HEADER_PADDING * 2

    // log all the dimension values
    Log.d(
        "calendar_imgen",
        "imageWidth: $imageWidth, imageHeight: $imageHeight, innerImgWidth: $innerImgWidth, innerImgHeight: $innerImgHeight, yearCalendarHeight: $yearCalendarHeight, yearHeaderHeight: $yearHeaderHeight, yearBlockHeight: $yearBlockHeight"
    )

    val opacityStops = {
        // only consider durations above 5 minutes
        val filteredDurations = dateToDurationInSeconds.values.filter {
            it > 60 * 5 // filter out durations less than or equal to 5 minutes
        }.map { it / 3600f } // convert to hours

        val avg = if (filteredDurations.isEmpty()) 8f else filteredDurations.average().toFloat()
        listOf(
            0f to 0.1f,
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
                Coord.Y -> fullHeaderHeight + (yearBlockHeight * yrIdx) + v.toFloat()
            }, coord
        )
    }

    fun toYearWeek(v: Number, yrIdx: Int, weekIdx: Int, coord: Coord = Coord.Y): Float {
        return toImage(
            when (coord) {
                Coord.X -> v.toFloat() + (CELL_SIZE + CELL_MARGIN) * weekIdx
                Coord.Y -> fullHeaderHeight + yearBlockHeight * yrIdx + yearHeaderHeight + v.toFloat()
            }, coord
        )
    }

    fun toYearCell(
        v: Number,
        yrIdx: Int,
        weekIdx: Int,
        dayIdx: Int,
        coord: Coord = Coord.Y
    ): Float {
        return toYearWeek(
            when (coord) {
                Coord.X -> v
                Coord.Y -> v.toFloat() + (CELL_SIZE + CELL_MARGIN) * dayIdx
            }, yrIdx, weekIdx, coord
        )
    }

    val years = mutableListOf<String>()

    // get the min and current years
    val currYear = LocalDate.now().year
    var minYear = dateToDurationInSeconds.keys.minOf { it.split("-")[0].toInt() }

    while (currYear - minYear < MIN_NUM_YEARS) {
        minYear -= 1
    }

    // add all the years to the list
    while (minYear <= currYear) {
        years.add(minYear.toString())
        minYear += 1
    }
    years.reverse()

    val totalHours = dateToDurationInSeconds.values.sum() / 3600f

    val imFont = ResourcesCompat.getFont(context, R.font.grotesk)

    return generateImage(
        "calendar_share_image",
        Size(imageWidth, imageHeight),
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

        val headerFontSize = HEADER_HEIGHT

        // on the left, the project name (or "Aggregate" if null)
        drawText(
            (projectName ?: "Aggregate").uppercase(),
            HEADER_PADDING, toImage(HEADER_HEIGHT + HEADER_PADDING, Coord.Y),
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = imageColors.foreground.toArgb()
                textSize = 40f
                isFakeBoldText = true
                textAlign = Paint.Align.LEFT
                typeface = imFont
            }
        )

        // on the right, the total hours
        drawText(
            "${totalHours.toInt()} HRS",
            imageWidth - PADDING - HEADER_PADDING,
            toImage(HEADER_PADDING + HEADER_HEIGHT, Coord.Y),
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = imageColors.foreground.toArgb()
                textSize = headerFontSize
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
                typeface = imFont
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
                toYearHeader(yearHeaderHeight - YEAR_LABEL_PADDING, yrIdx, Coord.Y),
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = imageColors.foreground.toArgb()
                    textSize = YEAR_LABEL_HEIGHT.toFloat()
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    typeface = imFont
                }
            )

            // get the first day of the year then move back to the Monday of said week
            val firstDayOfYear = java.time.LocalDate.of(year.toInt(), 1, 1)
            var currentDay = firstDayOfYear.minusDays(firstDayOfYear.dayOfWeek.value.toLong() - 1)

            // iterate through the weeks (in batches of 7 days) until we reach the next year
            var weekIdx = 0
            Log.d("calendar_imgen", "current year: $year")
            var sz = 0
            while (currentDay.year.toString() <= year) {
                sz += 1
                if (sz > 52) {
                    Log.d("calendar_imgen", "breaking at sz: $sz")
                    break
                }
                // for each day of the week
                for (dayIdx in 0 until 7) {
                    val dateStr = currentDay.toString() // "YYYY-MM-DD"
                    val durationInSeconds = dateToDurationInSeconds[dateStr] ?: 0
                    val durationInHours = durationInSeconds / 3600f

                    // check if the date is within the current year
                    if (currentDay.year.toString() == year) {


                        // determine opacity based on duration
                        val opacity =
                            linearInterpolation(durationInHours, opacityStops).coerceIn(0f, 1f)

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

                        val isFirstOfMonth = currentDay.dayOfMonth == 1
                        // write date, if first of month write shortened month
                        drawText(
                            if (isFirstOfMonth)
                                currentDay.month.toString().take(3) else
                                (currentDay.dayOfMonth).toString(),
                            toYearCell(CELL_SIZE / 2, yrIdx, weekIdx, dayIdx, Coord.X),
                            toYearCell(CELL_SIZE / 2, yrIdx, weekIdx, dayIdx, Coord.Y),
                            Paint(
                                Paint.ANTI_ALIAS_FLAG
                            ).apply {
                                color = imageColors.background.toArgb()
                                textSize = CELL_SIZE / (if (isFirstOfMonth) 3f else 2f)
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = isFirstOfMonth
                                typeface = imFont
                            }
                        )
                    }

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
