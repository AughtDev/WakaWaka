package com.aught.wakawaka.screens.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.aught.wakawaka.R
import com.aught.wakawaka.data.WakaHelpers
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


fun Paint.textProps(size: Float, color: Color, align: Paint.Align = Paint.Align.LEFT, isBold: Boolean = false, font: Typeface? = null): Paint {
    return this.apply {
        this.color = color.toArgb()
        textSize = size
        textAlign = align
        isFakeBoldText = isBold
        isAntiAlias = true

        if (font != null) {
            typeface = font
        }
    }
}

fun Paint.getTextBaselineOffset(): Float {
    val metrics = fontMetrics
    return (metrics.top + metrics.bottom) / 2
}

enum class YAlign {
    TOP, CENTER, BOTTOM
}

fun Canvas.drawAlignedText(
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    yAlign: YAlign = YAlign.BOTTOM
) {
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val yPos = when (yAlign) {
        YAlign.TOP -> y - bounds.top
        YAlign.CENTER -> y - paint.getTextBaselineOffset()
        YAlign.BOTTOM -> y - paint.fontMetrics.bottom
    }
    this.drawText(text, x, yPos, paint)
}

fun Canvas.drawProgressRoundRect(
    rect: RectF,
    cornerRadius: Float,
    strokeWidth: Float,
    progress: Float,
    progressColor: Color,
    backgroundColor: Color? = null
) {
    val path = Path().apply {
        // start from the top
        moveTo((rect.right - rect.left) / 2, rect.top)
        // move to top right corner minus border radius
        lineTo(rect.right - cornerRadius, rect.top)
        // quadratic curve to right side
        quadTo(rect.right, rect.top, rect.right, rect.top + cornerRadius)
        // line to bottom right corner minus border radius
        lineTo(rect.right, rect.bottom - cornerRadius)
        // quadratic curve to bottom side
        quadTo(rect.right, rect.bottom, rect.right - cornerRadius, rect.bottom)
        // line to bottom left corner plus border radius
        lineTo(rect.left + cornerRadius, rect.bottom)
        // quadratic curve to left side
        quadTo(rect.left, rect.bottom, rect.left, rect.bottom - cornerRadius)
        // line to top left corner plus border radius
        lineTo(rect.left, rect.top + cornerRadius)
        // quadratic curve to top side
        quadTo(rect.left, rect.top, rect.left + cornerRadius, rect.top)
        // close the path
        close()
    }


    val pathMeasure = android.graphics.PathMeasure(path, true)
    val pathLength = pathMeasure.length
    val progressLength = pathLength * progress

    val progressPath = Path()
    pathMeasure.getSegment(0f, progressLength, progressPath, true)

    val paint = Paint().apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        this.strokeCap = Paint.Cap.ROUND
        color = progressColor.toArgb()
        isAntiAlias = true
    }

    backgroundColor?.let {
        val backgroundPaint = Paint().apply {
            style = Paint.Style.FILL
            color = it.toArgb()
            isAntiAlias = true
        }

        drawPath(path, backgroundPaint)
    }
    drawPath(path, paint.apply {
        color = progressColor.copy(0.1f).toArgb()
    })
    drawPath(progressPath, paint)
}


fun Canvas.footer(
    context: Context,
    height: Float,
    xEnd: Float,
    yEnd: Float,
    fontSize: Float,
    imageColors: ImageColors,
) {
    val appAuthor = "@aughtdev"
    val appName = "WakaWaka"

    val imFont = ResourcesCompat.getFont(context, R.font.grotesk)

    val footerFontSize = height * 0.6f
    val footerFontSpacing = fontSize * 0.1f

    val authorPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).textProps(
        size = footerFontSize * 0.8f,
        color = imageColors.secondary,
        align = Paint.Align.RIGHT,
        isBold = true,
        font = imFont
    )
    drawAlignedText(
        appAuthor, xEnd, yEnd, authorPaint
    )


    val byXEnd = xEnd - footerFontSpacing - authorPaint.measureText(appAuthor)
    val byPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).textProps(
        size = footerFontSize * 0.6f,
        color = imageColors.foreground,
        align = Paint.Align.LEFT,
        isBold = true,
        font = imFont
    )
    drawAlignedText(
        "by", byXEnd, yEnd, byPaint
    )

    val appNameXEnd = byXEnd - footerFontSpacing - byPaint.measureText("by")
    val appNamePaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).textProps(
        size = footerFontSize,
        color = imageColors.primary,
        align = Paint.Align.RIGHT,
        isBold = true,
        font = imFont
    )
    drawAlignedText(
        appName, appNameXEnd, yEnd, appNamePaint
    )

    // icon to the left of the text
    // draw app icon
    val icon = ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, null)

    val iconWidth = height
    val iconHeight = height

    val iconXStart = appNameXEnd - footerFontSpacing - appNamePaint.measureText(appName) - iconWidth
    val iconY = yEnd - height / 2 - iconHeight / 2

    icon?.setBounds(iconXStart.toInt(), iconY.toInt(), (iconXStart + iconWidth).toInt(), (iconY + iconHeight).toInt())
    icon?.draw(this)
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

const val CELL_SIZE = 70f
const val CELL_MARGIN = 8f
const val CELL_RADIUS = 12f
const val IMAGE_PADDING = 40f

fun generateCalendarShareImage(
    context: Context,
    projectName: String?,
    imageColors: ImageColors,
    dateToDurationInSeconds: Map<String, Int>
): Uri? {

    val innerHeaderHeight = 150f
    val headerPadding = 20f

    val innerYearLabelHeight = 120f
    val yearLabelPadding = 15f

    val innerFooterHeight = 100f
    val footerPadding = 10f

    val minNumYears = 3

    val numYears = dateToDurationInSeconds.keys.map {
        it.split("-")[0].toIntOrNull() ?: 0
    }.distinct().size.coerceAtLeast(minNumYears)

    val imageWidth = (IMAGE_PADDING * 2 + (CELL_SIZE + CELL_MARGIN) * 53 - CELL_MARGIN).toInt()
    val imageHeight = (
            // padding
            IMAGE_PADDING * 2 +
                    // header
                    (innerHeaderHeight + headerPadding * 2) +
                    //  year labels
                    (innerYearLabelHeight + yearLabelPadding * 2) * numYears +
                    // year calendars
                    ((CELL_SIZE + CELL_MARGIN) * 7 - CELL_MARGIN) * numYears +
                    // footer
                    (innerFooterHeight + footerPadding * 2)
            ).toInt()

    val innerImgWidth = imageWidth - IMAGE_PADDING * 2
    val innerImgHeight = imageHeight - IMAGE_PADDING * 2

    val yearCalendarHeight = (CELL_SIZE + CELL_MARGIN) * 7 - CELL_MARGIN
    val yearHeaderHeight = innerYearLabelHeight + yearLabelPadding * 2
    val yearBlockHeight = yearHeaderHeight + yearCalendarHeight

    val fullHeaderHeight = innerHeaderHeight + headerPadding * 2
    val fullFooterHeight = innerFooterHeight + footerPadding * 2

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

    // region POSITIONING FUNCTIONS
    // ? ........................

    fun toImage(v: Number, coord: Coord = Coord.Y): Float {
        return when (coord) {
            Coord.X -> (IMAGE_PADDING + v.toFloat())
            Coord.Y -> (IMAGE_PADDING + v.toFloat())
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

    // ? ........................
    // endregion ........................


    val years = mutableListOf<String>()

    // get the min and current years
    val currYear = LocalDate.now().year
    var minYear = dateToDurationInSeconds.keys.minOf { it.split("-")[0].toInt() }

    while (currYear - minYear < minNumYears - 1) {
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

        val headerFontSize = innerHeaderHeight * 0.9f

        // on the left, the project name (or "Aggregate" if null)

        drawAlignedText(
            (projectName ?: "Aggregate").uppercase(),
            headerPadding + IMAGE_PADDING,
            toImage(innerHeaderHeight + headerPadding, Coord.Y),
            Paint(Paint.ANTI_ALIAS_FLAG).textProps(
                size = headerFontSize,
                color = imageColors.primary,
                align = Paint.Align.LEFT,
                isBold = true,
                font = imFont
            )
        )


        // on the right, the total hours
        drawAlignedText(
            totalHours.toInt().toString(),
            imageWidth - IMAGE_PADDING - headerPadding - headerFontSize * 1.7f,
            toImage(headerPadding + innerHeaderHeight, Coord.Y),
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).textProps(
                size = headerFontSize,
                color = imageColors.primary,
                align = Paint.Align.RIGHT,
                isBold = true,
                font = imFont
            )
        )
        drawAlignedText(
            "HRS",
            imageWidth - IMAGE_PADDING - headerPadding - headerFontSize * 1.55f,
            toImage(headerPadding + innerHeaderHeight - headerFontSize * 0.1, Coord.Y),
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).textProps(
                size = headerFontSize * 0.7f,
                color = imageColors.secondary,
                align = Paint.Align.LEFT,
                isBold = true,
                font = imFont
            )
        )

        // ? ........................
        // endregion ........................

        // a divider between header and years

        drawLine(
            IMAGE_PADDING,
            toImage(fullHeaderHeight, Coord.Y),
            imageWidth - IMAGE_PADDING,
            toImage(fullHeaderHeight, Coord.Y),
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
                color = imageColors.foreground.copy(alpha = 0.2f).toArgb()
            }
        )

        // region YEARS
        // ? ........................

        // for each year, iterate through each day and draw a cell
        for (yrIdx in years.indices) {
            val year = years[yrIdx]
            // draw year label
            drawAlignedText(
                year,
                toYearHeader(innerImgWidth / 2, yrIdx, Coord.X),
                toYearHeader(innerYearLabelHeight, yrIdx, Coord.Y),
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).textProps(
                    size = innerYearLabelHeight * 0.6f,
                    color = imageColors.foreground,
                    align = Paint.Align.CENTER,
                    isBold = true,
                    font = imFont
                )
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
                if (sz > 54) {
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

                        val isFirstOfMonth = currentDay.dayOfMonth == 1
                        val isInTheFuture = currentDay.isAfter(LocalDate.now())

                        // determine opacity based on duration, if currentDay is in the future, set to 0.05f
                        val opacity = if (isInTheFuture) 0.05f else
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

                        if (isInTheFuture) {
                            continue
                        }

                        var cellText = currentDay.dayOfMonth.toString()
                        var cellTextSize = CELL_SIZE / 2.5f
                        var cellTextColor = imageColors.background.toArgb()

                        if (isFirstOfMonth) {
                            cellText = currentDay.month.toString().take(3)
                            cellTextColor = imageColors.primary.copy(alpha = opacity).toArgb()
                            cellTextSize = CELL_SIZE / 3.2f

                            // draw a cell within the cell around the month text
                            drawRoundRect(
                                RectF(
                                    toYearCell(CELL_SIZE * 0.1f, yrIdx, weekIdx, dayIdx, Coord.X),
                                    toYearCell(CELL_SIZE * 0.1f, yrIdx, weekIdx, dayIdx, Coord.Y),
                                    toYearCell(CELL_SIZE * 0.9f, yrIdx, weekIdx, dayIdx, Coord.X),
                                    toYearCell(CELL_SIZE * 0.9f, yrIdx, weekIdx, dayIdx, Coord.Y),
                                ), CELL_RADIUS * 2 / 3, CELL_RADIUS * 2 / 3,
                                Paint().apply {
                                    style = Paint.Style.FILL
                                    isAntiAlias = true
                                    color = imageColors.background.toArgb()
                                }
                            )
                        }

                        // write date, if first of month write shortened month
                        drawAlignedText(
                            cellText,
                            toYearCell(CELL_SIZE / 2, yrIdx, weekIdx, dayIdx, Coord.X),
                            toYearCell(CELL_SIZE / 2, yrIdx, weekIdx, dayIdx, Coord.Y),
                            Paint(
                                Paint.ANTI_ALIAS_FLAG
                            ).textProps(
                                size = cellTextSize,
                                color = Color(cellTextColor),
                                align = Paint.Align.CENTER,
                                isBold = isFirstOfMonth,
                                font = imFont
                            ), YAlign.CENTER
                        )
                    }

                    currentDay = currentDay.plusDays(1)
                }
                weekIdx++
            }
        }


        // ? ........................
        // endregion ........................

        // region FOOTER
        // ? ........................

        // write out name of app
//        val footerFontSize = innerFooterHeight * 0.6f
//        val footerFontSpacing = footerFontSize * 0.1f
//
//        val wakawakaRightXPos = innerImgWidth - footerFontSize * 6f
//        val wakawakaPaint = Paint(
//            Paint.ANTI_ALIAS_FLAG
//        ).textProps(
//            size = footerFontSize,
//            color = imageColors.primary,
//            align = Paint.Align.RIGHT,
//            isBold = true,
//            font = imFont
//        )
//
//        // icon to the left of the text
//        // draw app icon
//        val icon = ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, null)
//        val iconWidth = fullFooterHeight
//        val iconHeight = fullFooterHeight
//        val iconX = toImage(wakawakaRightXPos - wakawakaPaint.measureText("WakaWaka") - footerFontSpacing - iconWidth, Coord.X)
//        val iconY = imageHeight - IMAGE_PADDING - innerFooterHeight
//
//        icon?.setBounds(iconX.toInt(), iconY.toInt(), (iconX + iconWidth).toInt(), (iconY + iconHeight).toInt())
//        icon?.draw(this)
//
//        drawAlignedText(
//            "WakaWaka",
//            toImage(wakawakaRightXPos, Coord.X),
//            toImage(innerImgHeight - footerPadding + footerFontSize * 0.1, Coord.Y),
//            wakawakaPaint
//        )
//
//        val byLeftXPos = wakawakaRightXPos + footerFontSpacing
//        val byPaint = Paint(
//            Paint.ANTI_ALIAS_FLAG
//        ).textProps(
//            size = footerFontSize * 0.6f,
//            color = imageColors.foreground,
//            align = Paint.Align.LEFT,
//            isBold = true,
//            font = imFont
//        )
//        drawAlignedText(
//            "by",
//            toImage(byLeftXPos, Coord.X),
//            toImage(innerImgHeight - footerPadding, Coord.Y),
//            byPaint
//        )
//
//        val aughtdevLeftXPos = byLeftXPos + byPaint.measureText("by") + footerFontSpacing
//        drawAlignedText(
//            "@aughtdev",
//            toImage(aughtdevLeftXPos, Coord.X),
//            toImage(innerImgHeight - footerPadding, Coord.Y),
//            Paint(
//                Paint.ANTI_ALIAS_FLAG
//            ).textProps(
//                size = footerFontSize * 0.8f,
//                color = imageColors.secondary,
//                align = Paint.Align.LEFT,
//                isBold = true,
//                font = imFont
//            )
//        )

        footer(
            context,
            innerFooterHeight,
            toImage(innerImgWidth, Coord.X),
            toImage(innerImgHeight - footerPadding, Coord.Y),
            innerFooterHeight * 0.6f, imageColors
        )


        // ? ........................
        // endregion ........................


    }
}

fun generateDailyShareImage(context: Context, dateToDurationInSeconds: Map<String, Int>): Uri? {


    return generateImage(
        "daily_share_image",
        Size(800, 400),
        context
    ) {

    }
}

data class ImageStreakData(
    val target: Float?,
    val streak: Int,
    val completion: Float,
)

enum class ProjectCardSection {
    HEADER, BADGE, TOTAL_HOURS, PROGRESS_BAR, STREAK_VALUES, STATS, FOOTER
}

fun generateSummaryCardImage(
    context: Context, projectName: String,
    totalHours: Float,
    dailyStreakData: ImageStreakData,
    weeklyStreakData: ImageStreakData,
    statToDurationInSeconds: List<Pair<String, Int>>,
    imageColors: ImageColors
): Uri? {

    val sectionHeights: List<Float> = listOf(
        50f, // header
        100f, // badge
        80f, // total hours
        40f, // progress bar
        150f, // streak values
        250f, // stats
        50f, // footer
    )

    val imageGapSize = 10f

    val aspectRatio = 2f

    val imageHeight = (
            sectionHeights.sum() + (imageGapSize * (sectionHeights.size - 1)) +
                    IMAGE_PADDING * 2
            ).toInt()
    val imageWidth = (imageHeight * aspectRatio).toInt()

    val innerImageHeight = imageHeight - IMAGE_PADDING * 2
    val innerImageWidth = imageWidth - IMAGE_PADDING * 2

    val progressBarWidthPercentage = 80f

    // region POSITIONING FUNCTIONS
    // ? ........................

    fun getSectionHeight(section: ProjectCardSection): Float {
        return sectionHeights[section.ordinal]
    }

    fun toImage(v: Number, coord: Coord = Coord.Y): Float {
        return when (coord) {
            Coord.X -> (IMAGE_PADDING + v.toFloat())
            Coord.Y -> (IMAGE_PADDING + v.toFloat())
        }
    }

    fun toSection(v: Number, section: ProjectCardSection, coord: Coord = Coord.Y): Float {
        return toImage(
            when (coord) {
                Coord.X -> v
                Coord.Y -> {
                    var yOff = 0f
                    for (i in ProjectCardSection.entries.indices) {
                        val sec = ProjectCardSection.entries[i]
                        if (sec == section) break
                        yOff += sectionHeights[i] + imageGapSize
                    }
                    yOff + v.toFloat()
                }
            }, coord
        )
    }

    // ? ........................
    // endregion ........................

    val imFont = ResourcesCompat.getFont(context, R.font.grotesk)



    return generateImage(
        "project_card_image",
        Size(imageWidth, imageHeight),
        context
    ) {
        // region HEADER
        // ? ........................

        val headerFontSize = getSectionHeight(ProjectCardSection.HEADER) * 0.6f
        // display the project name in the center

        drawAlignedText(
            projectName.uppercase(),
            toSection(innerImageWidth / 2, ProjectCardSection.HEADER, Coord.X),
            toSection(getSectionHeight(ProjectCardSection.HEADER) / 2, ProjectCardSection.HEADER, Coord.Y),
            Paint(Paint.ANTI_ALIAS_FLAG).textProps(
                size = headerFontSize,
                color = imageColors.primary,
                align = Paint.Align.CENTER,
                isBold = true,
                font = imFont
            ), YAlign.CENTER
        )

        // ? ........................
        // endregion ........................

        // region BADGE
        // ? ........................


        // ? ........................
        // endregion ........................


        // region TOTAL HOURS
        // ? ........................

        // write out total hours in the center with total and hours separately with different styling
        val hoursString = totalHours.toInt().toString()
        val hoursPaint = Paint(Paint.ANTI_ALIAS_FLAG).textProps(
            size = getSectionHeight(ProjectCardSection.TOTAL_HOURS) * 0.8f,
            color = imageColors.foreground,
            align = Paint.Align.LEFT,
            isBold = true,
            font = imFont
        )

        val labelString = "HRS"
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).textProps(
            size = getSectionHeight(ProjectCardSection.TOTAL_HOURS) * 0.4f,
            color = imageColors.secondary,
            align = Paint.Align.LEFT,
            isBold = true,
            font = imFont
        )

        val spacing = getSectionHeight(ProjectCardSection.TOTAL_HOURS) * 0.1f

        val txtWidth = hoursPaint.measureText(hoursString) + spacing + labelPaint.measureText(labelString)

        drawAlignedText(
            hoursString,
            toSection(innerImageWidth / 2 - txtWidth / 2, ProjectCardSection.TOTAL_HOURS, Coord.X),
            toSection(getSectionHeight(ProjectCardSection.TOTAL_HOURS) / 2, ProjectCardSection.TOTAL_HOURS, Coord.Y),
            hoursPaint, YAlign.CENTER
        )

        drawAlignedText(
            labelString,
            toSection(innerImageWidth / 2 - txtWidth / 2 + hoursPaint.measureText(hoursString) + spacing, ProjectCardSection.TOTAL_HOURS, Coord.X),
            toSection(getSectionHeight(ProjectCardSection.TOTAL_HOURS) / 2, ProjectCardSection.TOTAL_HOURS, Coord.Y),
            labelPaint, YAlign.CENTER
        )


        // ? ........................
        // endregion ........................


        // region PROGRESS BAR
        // ? ........................

        val progressBarThickness = getSectionHeight(ProjectCardSection.PROGRESS_BAR) / 3
        val progressBarSpacing = progressBarThickness / 2
        val progressBarFontSize = getSectionHeight(ProjectCardSection.PROGRESS_BAR) * 0.3f

        val progressBarWidth = (innerImageWidth * (progressBarWidthPercentage / 100f))

        val startX = toSection((innerImageWidth - progressBarWidth) / 2, ProjectCardSection.PROGRESS_BAR, Coord.X)
        val completionX = startX + progressBarWidth * (dailyStreakData.completion.coerceIn(0f, 1f))
        val endX = startX + progressBarWidth

        val startY = toSection(
            (getSectionHeight(ProjectCardSection.PROGRESS_BAR) - (progressBarThickness + progressBarSpacing + progressBarFontSize)) / 2,
            ProjectCardSection.PROGRESS_BAR,
            Coord.Y
        )
        val progressEndY = startY + progressBarThickness
        val textStartY = progressEndY + progressBarSpacing + progressBarFontSize

        drawRoundRect(
            RectF(
                startX, startY, endX, progressEndY,
            ), progressBarThickness / 2, progressBarThickness / 2,
            Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                color = imageColors.primary.copy(alpha = 0.1f).toArgb()
            }
        )
        drawRoundRect(
            RectF(
                startX, startY, completionX, progressEndY
            ), progressBarThickness / 2, progressBarThickness / 2,
            Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                color = imageColors.primary.toArgb()
            }
        )

        drawAlignedText(
            "100 hrs",
            endX, textStartY,
            Paint(Paint.ANTI_ALIAS_FLAG).textProps(
                size = progressBarFontSize,
                color = imageColors.primary,
                align = Paint.Align.CENTER,
                isBold = true,
                font = imFont
            ),
        )

        // ? ........................
        // endregion ........................

        // region STREAK VALUES
        // ? ........................

        val streakCellSize = getSectionHeight(ProjectCardSection.STREAK_VALUES) * 0.7f
        val streakCellCornerRadius = streakCellSize / 6
        val streakFontSize = streakCellSize * 0.8f
        val streakLabelFontSize = streakCellSize * 0.2f

        listOf(
            "Daily" to dailyStreakData,
            "Weekly" to weeklyStreakData,
        ).forEachIndexed { idx, pair ->
            val (label, data) = pair
            val cellCenterX = toSection(innerImageWidth / 4f + (innerImageWidth / 2f) * idx, ProjectCardSection.STREAK_VALUES, Coord.X)
            val cellCenterY = toSection(getSectionHeight(ProjectCardSection.STREAK_VALUES) / 2f, ProjectCardSection.STREAK_VALUES, Coord.Y)

            // draw cell
            drawRoundRect(
                RectF(
                    cellCenterX - streakCellSize / 2,
                    cellCenterY - streakCellSize / 2,
                    cellCenterX + streakCellSize / 2,
                    cellCenterY + streakCellSize / 2,
                ), streakCellCornerRadius, streakCellCornerRadius,
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    isAntiAlias = true
                    color = imageColors.primary.copy(alpha = 0.1f).toArgb()
                }
            )

            drawProgressRoundRect(
                RectF(
                    cellCenterX - streakCellSize / 2,
                    cellCenterY - streakCellSize / 2,
                    cellCenterX + streakCellSize / 2,
                    cellCenterY + streakCellSize / 2,
                ),
                streakCellCornerRadius,
                6f,
                data.completion.coerceIn(0f, 1f),
                imageColors.primary,
//                imageColors.primary.copy(alpha = 0.1f)
            )

            // draw streak number
            drawAlignedText(
                data.streak.toString(),
                cellCenterX,
                cellCenterY,
                Paint(Paint.ANTI_ALIAS_FLAG).textProps(
                    size = streakFontSize,
                    color = imageColors.primary,
                    align = Paint.Align.CENTER,
                    isBold = true,
                    font = imFont
                ), YAlign.CENTER
            )

            // draw label
            drawAlignedText(
                label.uppercase(),
                cellCenterX,
                cellCenterY + streakFontSize * 0.5f,
                Paint(Paint.ANTI_ALIAS_FLAG).textProps(
                    size = streakLabelFontSize,
                    color = imageColors.secondary,
                    align = Paint.Align.CENTER,
                    isBold = true,
                    font = imFont
                ), YAlign.CENTER
            )
        }

        // ? ........................
        // endregion ........................

        // region STATS
        // ? ........................

        val statHeight = getSectionHeight(ProjectCardSection.STATS) / statToDurationInSeconds.size
        val statPadding = statHeight * 0.1f
        val statFontSize = statHeight * 0.7f

        // split the stats into stats.size rows
        statToDurationInSeconds.forEachIndexed { idx, stat ->
            val (statLabel, statDuration) = stat

            val centerY = toSection(statHeight / 2 + statHeight * idx, ProjectCardSection.STATS, Coord.Y)

            drawAlignedText(
                statLabel,
                toSection(statPadding, ProjectCardSection.STATS, Coord.X),
                centerY,
                Paint(Paint.ANTI_ALIAS_FLAG).textProps(
                    size = statFontSize,
                    color = imageColors.foreground,
                    align = Paint.Align.LEFT,
                    isBold = false,
                    font = imFont
                ), YAlign.CENTER
            )

            drawAlignedText(
                WakaHelpers.durationInSecondsToDurationString(statDuration),
                toSection(innerImageWidth - statPadding, ProjectCardSection.STATS, Coord.X),
                centerY,
                Paint(Paint.ANTI_ALIAS_FLAG).textProps(
                    size = statFontSize,
                    color = imageColors.primary,
                    align = Paint.Align.RIGHT,
                    isBold = true,
                    font = imFont
                ), YAlign.CENTER
            )
        }


        // ? ........................
        // endregion ........................


        // region FOOTER
        // ? ........................

        val footerHeight = getSectionHeight(ProjectCardSection.FOOTER)
        val footerFontSize = footerHeight * 0.8f

        footer(
            context,
            footerHeight,
            toSection(innerImageWidth, ProjectCardSection.FOOTER, Coord.X),
            toSection(footerHeight, ProjectCardSection.FOOTER, Coord.X),
            footerFontSize, imageColors
        )

        // ? ........................
        // endregion ........................


    }
}
