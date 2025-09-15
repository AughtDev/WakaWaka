package com.aught.wakawaka.screens.badges

import android.graphics.Path
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import android.graphics.Typeface
import android.util.Log
import androidx.compose.ui.graphics.PathMeasure
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import com.aught.wakawaka.utils.ColorUtils


// region DRAWING UTILITIES
// -----------------------


fun Canvas.drawBadgeToCanvas(
    milestone: Milestone,
    x: Float, y: Float,
    width: Int, height: Int,
    fontSize: Float,
    sheenTranslate: Float,
    textFont: Typeface?
) {
    // Save the canvas state to ensure our transformations don't affect other drawings.
    withSave {

        // Translate the canvas to center the badge at the given (x, y) coordinates.
        // The drawing functions assume a top-left origin of (0, 0).
        val badgeColor = ColorUtils.hexToColor(milestone.colorHex).copy(0.3f).toArgb()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSize
            this.color = badgeColor
            textAlign = Paint.Align.CENTER
//        typeface = textFont
            isFakeBoldText = true
        }
        val badgeWidth = textPaint.measureText("${milestone.hours}h").toInt()

        translate(x - badgeWidth / 2f, y - height / 2f)

        drawHoursText(milestone.hours, badgeWidth, height, textPaint)

        milestone.badge(this, badgeColor, badgeWidth, (height - fontSize).toInt())

        drawShinyRect(badgeColor, badgeWidth, height, sheenTranslate)

        // Restore the canvas to its original state.
    }
}

/**
 * Draws a rectangular sheen effect onto the canvas using a gradient and a blend mode.
 * This is the equivalent of the `shiny` modifier in a Compose `drawWithCache` block.
 *
 * @param color The base color of the badge, used for the sheen gradient.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 * @param translate The normalized sheen position from 0f to 1f.
 */
fun Canvas.drawShinyRect(color: Int, width: Int, height: Int, translate: Float) {
    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    sheenPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    // The sheen will be a diagonal gradient
    val sheenShader = LinearGradient(
        0f + width * translate,
        0f,
        width * 2 + width * translate,
        height / 2f,
        intArrayOf(
            Color.argb(
                Color.alpha(color) / 2,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ),
            Color.argb(
                Color.alpha(color) / 2,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ),
            Color.argb(
                Color.alpha(color) * 4 / 10,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ),
            Color.rgb(Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(
                Color.alpha(color) * 4 / 10,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ),
            Color.argb(
                Color.alpha(color) / 2,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ),
            Color.argb(
                Color.alpha(color) / 2,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ),
        ),
        null,
        Shader.TileMode.CLAMP
    )
    sheenPaint.shader = sheenShader

    // Save a layer to ensure the blending is applied only to the badge.
    saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
    drawRect(0f, 0f, width.toFloat(), height.toFloat(), sheenPaint)
    restore()
}

/**
 * A utility function to draw the hours text below the crown.
 *
 * @param canvas The canvas to draw onto.
 * @param numHours The number of hours to display.
 * @param color The color of the text.
 * @param size The size of the text and badge.
 */
fun Canvas.drawHoursText(
    numHours: Int,
    width: Int,
    height: Int,
    paint: Paint
): Float {
    val x = width / 2f
    val y = height.toFloat() + paint.fontMetrics.bottom
    Log.d("drawHoursText", "numHours: $numHours, x: $x, y: $y")
    drawText("${numHours}h", x, y, paint)
    return paint.measureText("${numHours}h")
}

// endregion

// region CANVAS CROWNS
// ? ........................

/**
 * A utility function to set up the paint object for the crowns.
 */
fun getCrownPaint(color: Int, size: Int, style: Paint.Style): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = size / 5f
        strokeCap = Paint.Cap.ROUND
        this.style = style
    }
}

/**
 * Draws the bronze crown onto a Canvas.
 * @param canvas The canvas to draw on.
 * @param color The color of the crown.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 */
fun Canvas.drawBronzeCrown(color: Int, width: Int, height: Int) {
    val sz = width.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw * 3 / 2f, sz * 3 / 4f + yOff)
        // From DrawScope quadraticTo(w / 2, sz * 3 / 4f + yOff, w / 2, sw + yOff)
        // To Canvas quadraticTo(x1, y1, x2, y2)
        quadTo(width / 2f, sz * 3 / 4f + yOff, width / 2f, sw + yOff)
        // From DrawScope quadraticTo(w / 2, sz * 3 / 4f + yOff, w - sw * 3 / 2f, sz * 3 / 4f + yOff)
        // To Canvas quadraticTo(x1, y1, x2, y2)
        quadTo(width / 2f, sz * 3 / 4f + yOff, width - sw * 3 / 2f, sz * 3 / 4f + yOff)
    }
    drawPath(crownPath, getCrownPaint(color, width, Paint.Style.STROKE))
    drawCircle(
        width / 2f,
        -sw / 2,
        sw,
        getCrownPaint(color, height, Paint.Style.FILL)
    )
}

/**
 * Draws the silver crown onto a Canvas.
 * @param canvas The canvas to draw on.
 * @param color The color of the crown.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 */
fun Canvas.drawSilverCrown(color: Int, width: Int, height: Int) {
    val sz = height.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw * 3 / 2f, sz * 3 / 4f + yOff)
        lineTo(sw * 3 / 2f, sw + yOff)
        quadTo(width / 2f, sz + 1 + yOff, width - sw * 3 / 2f, sw + yOff)
        lineTo(width - sw * 3 / 2f, sz * 3 / 4f + yOff)
    }
    drawPath(crownPath, getCrownPaint(color, width, Paint.Style.STROKE))
    drawCircle(
        width / 2f,
        -sw + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.FILL)
    )
}

/**
 * Draws the gold crown onto a Canvas.
 * @param canvas The canvas to draw on.
 * @param color The color of the crown.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 */
fun Canvas.drawGoldCrown(color: Int, width: Int, height: Int) {
    val sz = height.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadTo(width / 2f, sz + 1 + yOff, width - sw, sw + yOff)
        lineTo(width - sw, sz + yOff)
    }
    drawPath(crownPath, getCrownPaint(color, width, Paint.Style.STROKE))
    drawCircle(
        sw,
        -sw * 1 + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.FILL)
    )
    drawCircle(
        width / 2f,
        -sw * 2 + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.STROKE)
    )
    drawCircle(
        width - sw,
        -sw * 1 + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.FILL)
    )
}

/**
 * Draws the diamond crown onto a Canvas.
 * @param canvas The canvas to draw on.
 * @param color The color of the crown.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 */
fun Canvas.drawDiamondCrown(color: Int, width: Int, height: Int) {
    val sz = height.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadTo(width / 4f, sz + yOff, width / 2f, 0f + yOff)
        quadTo(3 * width / 4f, sz + yOff, width - sw, sw + yOff)
        lineTo(width - sw, sz + yOff)
    }
    drawPath(crownPath, getCrownPaint(color, width, Paint.Style.STROKE))
    drawCircle(
        sw,
        -sw * 4 / 3f + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.FILL)
    )
    drawCircle(
        width - sw,
        -sw * 4 / 3f + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.FILL)
    )
}

/**
 * Draws the royal purple crown onto a Canvas.
 * @param canvas The canvas to draw on.
 * @param color The color of the crown.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 */
fun Canvas.drawRoyalPurpleCrown(color: Int, width: Int, height: Int) {
    val sz = height.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadTo(width / 4f, sz + yOff, width / 2f, 0f + yOff)
        quadTo(3 * width / 4f, sz + yOff, width - sw, sw + yOff)
        lineTo(width - sw, sz + yOff)
    }
    drawPath(crownPath, getCrownPaint(color, width, Paint.Style.STROKE))
    drawCircle(
        sw,
        -sw * 4 / 3f + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.FILL)
    )
    drawCircle(
        width / 2f,
        -sw * 3f + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.STROKE)
    )
    drawCircle(
        width - sw,
        -sw * 4 / 3f + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.FILL)
    )
}

/**
 * Draws the royal red crown onto a Canvas.
 * @param canvas The canvas to draw on.
 * @param color The color of the crown.
 * @param width The width of the drawing area.
 * @param height The height of the drawing area.
 */
fun Canvas.drawRoyalRedCrown(color: Int, width: Int, height: Int) {
    val sz = height.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw / 2, sz + yOff)
        lineTo(sw / 2, sw * 2 + yOff)
        quadTo(width / 8f, sz + yOff, width / 4f, sw * 3 / 2 + yOff)
        quadTo(width / 2f, sz + yOff, 3 * width / 4f, sw + yOff)
        quadTo(7 * width / 8f, sz + yOff, width - sw / 2, sw * 3 / 2 + yOff)
        lineTo(width - sw / 2, sz + yOff)
    }
    val gemPath = Path().apply {
        moveTo(width / 2f - sw, -sz / 2 + yOff)
        lineTo(width / 2f, -sz * 4 / 5f + yOff)
        lineTo(width / 2f + sw, -sz / 2 + yOff)
        lineTo(width / 2f, -sz * 1 / 5f + yOff)
        close()
    }
    drawPath(crownPath, getCrownPaint(color, width, Paint.Style.STROKE))
    drawPath(
        gemPath,
        getCrownPaint(color, width, Paint.Style.STROKE).apply { strokeWidth = sw * 2 / 3f })
    drawCircle(sw, -sw + yOff, sw * 2 / 3f, getCrownPaint(color, width, Paint.Style.FILL))
    drawCircle(
        width / 4f,
        -sw * 4 / 3f + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.STROKE).apply { strokeWidth = sw * 2 / 3f })
    drawCircle(
        3 * width / 4f,
        -sw * 4 / 3f + yOff,
        sw,
        getCrownPaint(color, width, Paint.Style.STROKE).apply { strokeWidth = sw * 2 / 3f })
    drawCircle(width - sw, -sw + yOff, sw * 2 / 3f, getCrownPaint(color, width, Paint.Style.FILL))
}

// ? ........................
// endregion ........................

