package com.aught.wakawaka.screens.badges

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.withSave
import com.aught.wakawaka.utils.ColorUtils

// region DRAWING UTILITIES
// -----------------------

/**
 * Draws the complete milestone badge onto a Canvas at a specified center point.
 * This function orchestrates the drawing of the crown, text, and underline,
 * and applies a metallic sheen effect over the final result.
 *
 * @param milestone The milestone data, containing hours, color, and drawing functions.
 * @param canvas The Android graphics Canvas to draw on.
 * @param cx The horizontal center coordinate for the badge.
 * @param cy The vertical center coordinate for the badge.
 * @param width The total width of the badge drawing area.
 * @param height The total height of the badge drawing area.
 * @param sheenTranslate A value from -2.0 to 1.0 controlling the horizontal position of the sheen.
 * @param textFont Optional custom Typeface for the text.
 */
fun drawBadgeToCanvas(
    milestone: Milestone,
    canvas: Canvas,
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    sheenTranslate: Float,
    textFont: Typeface?
) {
    // 1. SETUP
    // -----------------------
    val badgeColor = ColorUtils.hexToColor(milestone.colorHex).toArgb()
    // Create a semi-transparent version for the crown, matching the original composable
    val crownColor = Color.argb(
        (0.4f * 255).toInt(),
        Color.red(badgeColor),
        Color.green(badgeColor),
        Color.blue(badgeColor)
    )
    // Create a text color version, matching the original composable
    val textColor = Color.argb(
        (0.6f * 255).toInt(),
        Color.red(badgeColor),
        Color.green(badgeColor),
        Color.blue(badgeColor)
    )


    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = textColor
        this.textSize = height * 0.5f // Use 90% of available height for the main number
        this.textAlign = Paint.Align.CENTER
        this.typeface = typeface ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        this.isFakeBoldText = true
    }

//     Smaller text paint for the "h" unit
    val unitPaint = Paint(textPaint).apply {
        this.textSize = textPaint.textSize * 2 / 3f // 'h' is 2/3 the size
        this.isFakeBoldText = false
    }

    val width = (textPaint.measureText(milestone.hours.toString()) + unitPaint.measureText("h"))

    val left = cx - width / 2f
    val top = cy - height / 2f
    val bounds = RectF(left, top, left + width, top + height)

    // 2. MAIN DRAWING LOGIC
    // -----------------------
    // Save the canvas and create a new, temporary layer to draw the badge onto.
    val layer = canvas.saveLayer(bounds, null)

    // Draw all the badge components (crown, text, underline) onto the layer.
    canvas.withSave {
        translate(left, top)

        val crownHeight = height * 0.40f
        val textHeight = height * 0.50f
        val underlineHeight = height * 0.10f

        // Draw Crown
        canvas.withSave {
            milestone.badge(canvas, crownColor, width, crownHeight)
        }

        // Draw Text
        canvas.withSave {
            translate(0f, crownHeight)
            drawHoursText(
                canvas = this,
                numHours = milestone.hours,
                width = width,
                height = textHeight,
                textPaint = textPaint,
                unitPaint = unitPaint
            )
        }

        // Draw Underline
        canvas.withSave {
            translate(0f, crownHeight + textHeight)
            drawUnderline(
                canvas = this,
                color = badgeColor,
                width = width,
                height = underlineHeight
            )
        }
    }

    // 3. APPLY SHEEN EFFECT
    // -----------------------
    // Now, draw the sheen gradient directly onto the same layer.
    // The SRC_IN mode will use the badge components we just drew as a mask.
    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        shader = LinearGradient(
            bounds.left + (bounds.width() * 1f),
            bounds.top,
            bounds.left + (bounds.width() * 0f),
            bounds.top + bounds.height(),
            intArrayOf(
                badgeColor.copy(alpha = 0.5f),
                badgeColor.copy(alpha = 0.4f),
                badgeColor,
                badgeColor.copy(alpha = 0.4f),
                badgeColor.copy(alpha = 0.5f),
            ),
            floatArrayOf(0f, 0.4f, 0.5f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(bounds, sheenPaint)
//    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//        shader = LinearGradient(
//            width * sheenTranslate,
//            0f,
//            (width * sheenTranslate) + width,
//            height,
//            intArrayOf(
//                Color.argb(Color.alpha(badgeColor) / 2, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)),
//                Color.argb(Color.alpha(badgeColor) / 2, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)),
//                Color.argb(Color.alpha(badgeColor) * 4 / 10, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)),
//                badgeColor,
//                Color.argb(Color.alpha(badgeColor) * 4 / 10, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)),
//                Color.argb(Color.alpha(badgeColor) / 2, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)),
//                Color.argb(Color.alpha(badgeColor) / 2, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)),
//            ),
//            null,
//            Shader.TileMode.CLAMP
//        )
//    }
//    canvas.drawRect(0f, 0f, width, height, sheenPaint)

    // 4. MERGE THE LAYER
    // -----------------------
    // Finally, restore the canvas. This merges our completed layer (badge + sheen)
    // onto the main canvas, making it visible.
    canvas.restoreToCount(layer)
}

/**
 * Draws the hours text, splitting the number and the "h" unit for separate styling,
 * closely mimicking the original Composable's appearance.
 */
private fun drawHoursText(
    canvas: Canvas,
    numHours: Int,
//    color: Int,
    width: Float,
    height: Float,
//    typeface: Typeface?
    textPaint: Paint,
    unitPaint: Paint
) {
    val hoursText = "$numHours"
    val unitText = "h"
    val hoursWidth = textPaint.measureText(hoursText)
    val unitWidth = unitPaint.measureText(unitText)
    val totalTextWidth = hoursWidth + unitWidth

    // Calculate starting positions to center the combined text
    val startX = (width - totalTextWidth) / 2f
//    val startX = 0f
    val yOff = height * 0.05f // Slight vertical offset to fine-tune centering
    val textY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f) + yOff
//    val unitY = textY + height * 0.15f // Offset 'h' slightly down
    val unitY = textY - 0.1f * height

    canvas.drawText(hoursText, startX + hoursWidth / 2, textY, textPaint)
    canvas.drawText(unitText, startX + hoursWidth + unitWidth / 2, unitY, unitPaint)
}

/**
 * Draws the underline beneath the text.
 */
private fun drawUnderline(canvas: Canvas, color: Int, width: Float, height: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.alpha = (0.3f * 255).toInt()
        strokeWidth = height * 0.7f
        strokeCap = Paint.Cap.ROUND
    }
    val padding = width * 0.1f // 10% padding on each side
    val y = height / 2f
    canvas.drawLine(padding, y, width - padding, y, paint)
}

/**
 * Draws a rectangular sheen effect using a gradient and a blend mode.
 */
private fun drawSheen(
    canvas: Canvas,
    color: Int,
    width: Float,
    height: Float,
    translate: Float,
    bounds: RectF
) {
    val sheenColors = intArrayOf(
        Color.TRANSPARENT,
        Color.WHITE.copy(alpha = 0.5f),
        Color.TRANSPARENT
    )
    // The animated position of the sheen gradient
    val sheenWidth = width * 0.5f
    val startX = (bounds.left - sheenWidth) + (width + sheenWidth) * translate
    val endX = startX + sheenWidth

    val sheenShader = LinearGradient(
        startX,
        bounds.top,
        endX,
        bounds.bottom,
        sheenColors,
        null,
        Shader.TileMode.CLAMP
    )

    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = sheenShader
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    canvas.drawRect(bounds, sheenPaint)
}

// Extension function to allow copying Jetpack Compose Colors to Android Colors
private fun Int.copy(alpha: Float): Int {
    return Color.argb(
        (alpha * 255.0f).toInt(),
        Color.red(this),
        Color.green(this),
        Color.blue(this)
    )
}
// endregion

// region CANVAS CROWNS
// ? ........................

/**
 * A utility function to create a Paint object for drawing crowns.
 */
private fun getCrownPaint(color: Int, strokeWidth: Float, style: Paint.Style): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.strokeWidth = strokeWidth
        this.strokeCap = Paint.Cap.ROUND
        this.style = style
    }
}

/**
 * Draws the bronze crown onto a Canvas.
 * @param color The ARGB color of the crown.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 */
fun Canvas.drawBronzeCrown(color: Int, width: Float, height: Float) {
    // 1. Define the crown's proportions based on the available height.
    val sw = height / 5f // This is our base unit for size and stroke width.
    val yOff = height / 5f

    // 2. Define the original vertical coordinates of the shape, including the negative space.
    val originalCircleCenterY = -sw / 2f
    val originalPathY1 = height * 0.75f + yOff // The lowest point of the crown's path.
    val originalPathY2 = sw + yOff             // The highest point of the crown's path.

    // 3. Find the exact top and bottom bounds of the original drawing.
    val topBound = originalCircleCenterY - sw // The very top of the circle (center - radius).
    val bottomBound = originalPathY1          // The bottom of the path is the lowest point.
    val totalOriginalHeight = bottomBound - topBound

    // 4. Calculate the scaling factor needed to compress the drawing to fit the canvas `height`.
    val scale = height / (totalOriginalHeight + sw)

    // 5. Create a helper lambda to remap all original Y coordinates.
    // This function shifts the drawing so it starts at y=0 and then scales it to fit.
    val remapY = { originalY: Float -> (originalY - topBound) * scale + sw * scale }

    // 6. Rebuild the crown using the remapped coordinates and scaled dimensions.
    val scaledSw = sw * scale // The stroke width and radius must also be scaled.

    val crownPath = Path().apply {
        // Use the original horizontal proportions but the remapped vertical positions.
        moveTo(sw * 1.5f, remapY(originalPathY1))
        quadTo(width / 2f, remapY(originalPathY1), width / 2f, remapY(originalPathY2))
        quadTo(width / 2f, remapY(originalPathY1), width - sw * 1.5f, remapY(originalPathY1))
    }

    // Draw the path and circle using the new scaled and remapped values.
    drawPath(crownPath, getCrownPaint(color, scaledSw, Paint.Style.STROKE))
    drawCircle(
        width / 2f,
        remapY(originalCircleCenterY),
        scaledSw,
        getCrownPaint(color, scaledSw, Paint.Style.FILL)
    )
}

/**
 * Draws the silver crown onto a Canvas.
 */
fun Canvas.drawSilverCrown(color: Int, width: Float, height: Float) {
    // 1. Define proportions.
    val sw = height / 5f
    val yOff = height / 5f

    // 2. Define original Y coordinates.
    val originalCircleCenterY = -sw + yOff
    val originalPathYSide = sw + yOff
    val originalPathYBottom = height * 0.75f + yOff
    val originalPathYControl = height + 1f + yOff // The curve's control point is the lowest point.

    // 3. Find the exact vertical bounds of the original drawing.
    val topBound = originalCircleCenterY - sw // Top of the circle (center - radius).
    val bottomBound = originalPathYControl
    val totalOriginalHeight = bottomBound - topBound

    // 4. Calculate the scaling factor.
//    val scale = height / totalOriginalHeight
    val scale = height / (totalOriginalHeight + sw)

    // 5. Create the remapping helper function.
//    val remapY = { originalY: Float -> (originalY - topBound) * scale }
    val remapY = { originalY: Float -> (originalY - topBound) * scale + sw * scale }

    // 6. Rebuild the crown using remapped values.
    val scaledSw = sw * scale
    val crownPath = Path().apply {
        moveTo(sw * 1.5f, remapY(originalPathYBottom))
        lineTo(sw * 1.5f, remapY(originalPathYSide))
        quadTo(width / 2f, remapY(originalPathYControl), width - sw * 1.5f, remapY(originalPathYSide))
        lineTo(width - sw * 1.5f, remapY(originalPathYBottom))
    }

    drawPath(crownPath, getCrownPaint(color, scaledSw, Paint.Style.STROKE))
    drawCircle(
        width / 2f,
        remapY(originalCircleCenterY),
        scaledSw,
        getCrownPaint(color, scaledSw, Paint.Style.FILL)
    )
}

/**
 * Draws the gold crown onto a Canvas.
 */
fun Canvas.drawGoldCrown(color: Int, width: Float, height: Float) {
    // 1. Define proportions.
    val sw = height / 5f
    val yOff = height / 5f

    // 2. Define original Y coordinates.
    val originalSideCircleCenterY = -sw + yOff
    val originalCenterCircleCenterY = -sw * 2f + yOff // Highest circle.
    val originalPathYSide = sw + yOff
    val originalPathYBottom = height + yOff
    val originalPathYControl = height + 1f + yOff // Lowest point.

    // 3. Find the exact vertical bounds.
    val topBound = originalCenterCircleCenterY - sw // Top of the highest circle.
    val bottomBound = originalPathYControl
    val totalOriginalHeight = bottomBound - topBound

    // 4. Calculate the scaling factor.
//    val scale = height / totalOriginalHeight
    val scale = height / (totalOriginalHeight + sw)

    // 5. Create the remapping helper function.
//    val remapY = { originalY: Float -> (originalY - topBound) * scale }
    val remapY = { originalY: Float -> (originalY - topBound) * scale + sw * scale }

    // 6. Rebuild the crown using remapped values.
    val scaledSw = sw * scale
    val crownPath = Path().apply {
        moveTo(sw, remapY(originalPathYBottom))
        lineTo(sw, remapY(originalPathYSide))
        quadTo(width / 2f, remapY(originalPathYControl), width - sw, remapY(originalPathYSide))
        lineTo(width - sw, remapY(originalPathYBottom))
    }

    val paintStroke = getCrownPaint(color, scaledSw, Paint.Style.STROKE)
    val paintFill = getCrownPaint(color, scaledSw, Paint.Style.FILL)

    drawPath(crownPath, paintStroke)
    // Draw the three circles using their remapped vertical positions.
    drawCircle(sw, remapY(originalSideCircleCenterY), scaledSw, paintFill)
    drawCircle(width / 2f, remapY(originalCenterCircleCenterY), scaledSw, paintStroke)
    drawCircle(width - sw, remapY(originalSideCircleCenterY), scaledSw, paintFill)
}

/**
 * Draws the diamond crown onto a Canvas.
 */
fun Canvas.drawDiamondCrown(color: Int, width: Float, height: Float) {
    // 1. Define proportions.
    val sw = height / 5f
    val yOff = height / 5f

    // 2. Define original Y coordinates.
    val originalCircleCenterY = -sw * 1.33f + yOff
    val originalPathYTop = yOff
    val originalPathYSide = sw + yOff
    val originalPathYBottom = height + yOff

    // 3. Find the exact vertical bounds of the original drawing.
    val topBound = originalCircleCenterY - sw // Top of circle (center - radius) is the highest point.
    val bottomBound = originalPathYBottom
    val totalOriginalHeight = bottomBound - topBound

    // 4. Calculate the scaling factor and create the remapping helper.
//    val scale = height / totalOriginalHeight
    val scale = height / (totalOriginalHeight + sw)
//    val remapY = { originalY: Float -> (originalY - topBound) * scale }
    val remapY = { originalY: Float -> (originalY - topBound) * scale + sw * scale }

    // 5. Rebuild the crown using remapped values.
    val scaledSw = sw * scale
    val crownPath = Path().apply {
        moveTo(sw, remapY(originalPathYBottom))
        lineTo(sw, remapY(originalPathYSide))
        quadTo(width / 4f, remapY(originalPathYBottom), width / 2f, remapY(originalPathYTop))
        quadTo(width * 3f / 4f, remapY(originalPathYBottom), width - sw, remapY(originalPathYSide))
        lineTo(width - sw, remapY(originalPathYBottom))
    }

    val paintStroke = getCrownPaint(color, scaledSw, Paint.Style.STROKE)
    val paintFill = getCrownPaint(color, scaledSw, Paint.Style.FILL)

    drawPath(crownPath, paintStroke)
    drawCircle(sw, remapY(originalCircleCenterY), scaledSw, paintFill)
    drawCircle(width - sw, remapY(originalCircleCenterY), scaledSw, paintFill)
}

/**
 * Draws the royal purple crown onto a Canvas.
 */
fun Canvas.drawRoyalPurpleCrown(color: Int, width: Float, height: Float) {
    // 1. Define proportions.
    val sw = height / 5f
    val yOff = height / 5f

    // 2. Define original Y coordinates.
    val originalSideCircleCenterY = -sw * 1.33f + yOff
    val originalCenterCircleCenterY = -sw * 3f + yOff // The highest circle.
    val originalPathYTop = yOff
    val originalPathYSide = sw + yOff
    val originalPathYBottom = height + yOff

    // 3. Find the exact vertical bounds.
    val topBound = originalCenterCircleCenterY - sw // Top of the highest circle.
    val bottomBound = originalPathYBottom
    val totalOriginalHeight = bottomBound - topBound

    // 4. Calculate the scaling factor and create the remapping helper.
//    val scale = height / totalOriginalHeight
    val scale = height / (totalOriginalHeight + sw)
//    val remapY = { originalY: Float -> (originalY - topBound) * scale }
    val remapY = { originalY: Float -> (originalY - topBound) * scale + sw * scale }

    // 5. Rebuild the crown using remapped values.
    val scaledSw = sw * scale
    val crownPath = Path().apply {
        moveTo(sw, remapY(originalPathYBottom))
        lineTo(sw, remapY(originalPathYSide))
        quadTo(width / 4f, remapY(originalPathYBottom), width / 2f, remapY(originalPathYTop))
        quadTo(width * 3f / 4f, remapY(originalPathYBottom), width - sw, remapY(originalPathYSide))
        lineTo(width - sw, remapY(originalPathYBottom))
    }

    val paintStroke = getCrownPaint(color, scaledSw, Paint.Style.STROKE)
    val paintFill = getCrownPaint(color, scaledSw, Paint.Style.FILL)

    drawPath(crownPath, paintStroke)
    drawCircle(sw, remapY(originalSideCircleCenterY), scaledSw, paintFill)
    drawCircle(width / 2f, remapY(originalCenterCircleCenterY), scaledSw, paintStroke)
    drawCircle(width - sw, remapY(originalSideCircleCenterY), scaledSw, paintFill)
}

/**
 * Draws the royal red crown onto a Canvas.
 */
fun Canvas.drawRoyalRedCrown(color: Int, width: Float, height: Float) {
    // 1. Define proportions.
    val sw = height / 5f
    val yOff = height / 5f

    // 2. Define original Y coordinates for all shapes.
    val originalGemPathYTop = -height * 0.8f + yOff // Highest point of the gem.
    val originalGemPathYMid1 = -height * 0.5f + yOff
    val originalGemPathYMid2 = -height * 0.2f + yOff
    val originalCrownPathYBottom = height + yOff // Lowest point.
    // ... other path and circle Y values fall between these bounds.

    // 3. Find the exact vertical bounds.
    val topBound = originalGemPathYTop
    val bottomBound = originalCrownPathYBottom
    val totalOriginalHeight = bottomBound - topBound

    // 4. Calculate the scaling factor and create the remapping helper.
//    val scale = height / totalOriginalHeight
    val scale = height / (totalOriginalHeight + sw)
//    val remapY = { originalY: Float -> (originalY - topBound) * scale }
    val remapY = { originalY: Float -> (originalY - topBound) * scale + sw * scale }

    // 5. Rebuild the crown using remapped values.
    val scaledSw = sw * scale
    val scaledGemStroke = sw * 0.66f * scale

    val crownPath = Path().apply {
        moveTo(sw / 2f, remapY(height + yOff))
        lineTo(sw / 2f, remapY(sw * 2f + yOff))
        quadTo(width / 8f, remapY(height + yOff), width / 4f, remapY(sw * 1.5f + yOff))
        quadTo(width / 2f, remapY(height + yOff), width * 3f / 4f, remapY(sw + yOff))
        quadTo(width * 7f / 8f, remapY(height + yOff), width - sw / 2f, remapY(sw * 1.5f + yOff))
        lineTo(width - sw / 2f, remapY(height + yOff))
    }
    val gemPath = Path().apply {
        moveTo(width / 2f - sw, remapY(originalGemPathYMid1))
        lineTo(width / 2f, remapY(originalGemPathYTop))
        lineTo(width / 2f + sw, remapY(originalGemPathYMid1))
        lineTo(width / 2f, remapY(originalGemPathYMid2))
        close()
    }

    val mainPaint = getCrownPaint(color, scaledSw, Paint.Style.STROKE)
    val gemPaint = getCrownPaint(color, scaledGemStroke, Paint.Style.STROKE)
    val fillPaint = getCrownPaint(color, scaledSw, Paint.Style.FILL)

    drawPath(crownPath, mainPaint)
    drawPath(gemPath, gemPaint)

    drawCircle(sw, remapY(-sw + yOff), scaledGemStroke, fillPaint)
    drawCircle(width / 4f, remapY(-sw * 1.33f + yOff), scaledSw, gemPaint)
    drawCircle(width * 3f / 4f, remapY(-sw * 1.33f + yOff), scaledSw, gemPaint)
    drawCircle(width - sw, remapY(-sw + yOff), scaledGemStroke, fillPaint)
}

// ? ........................
// endregion ........................
