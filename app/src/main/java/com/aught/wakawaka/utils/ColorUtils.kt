package com.aught.wakawaka.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColor
import androidx.core.graphics.toRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

class ColorUtils {
    companion object {
        // region GENERAL COLOR FUNCTIONS
        /**
         * Generates a contrasting color suitable for text on top of the given background color,
         * taking into account potential opacity changes.
         *
         * @param backgroundColor The background color
         * @param contrastRatio The minimum contrast ratio to achieve (recommended 4.5 for WCAG AA)
         * @return A color suitable for text that contrasts with the background
         */
        fun getContrastingTextColor(backgroundColor: Color, contrastRatio: Float = 4.5f): Color {
            // Calculate color luminance (brightness perception)
            val luminance = calculateLuminance(backgroundColor)

            // Start with either black or white based on background luminance
            var textColor = if (luminance > 0.5) Color.Black else Color.White

            // For very light colors with potential transparency, ensure black will still contrast
            // For very dark colors with potential transparency, ensure white will still contrast
            if (luminance > 0.85) {
                // Very light color - needs deeper black to handle transparency cases
                textColor = Color(0xFF000000)  // Pure black
            } else if (luminance < 0.15) {
                // Very dark color - needs brighter white to handle transparency cases
                textColor = Color(0xFFFFFFFF)  // Pure white
            } else if (luminance > 0.5 && luminance <= 0.85) {
                // Medium-light color - create a darker version
                textColor = darken(backgroundColor, 0.6f)
            } else if (luminance >= 0.15 && luminance <= 0.5) {
                // Medium-dark color - create a lighter version
                textColor = lighten(backgroundColor, 0.6f)
            }

            // Verify and adjust the contrast if needed
            val actualContrast = calculateContrastRatio(backgroundColor, textColor)
            if (actualContrast < contrastRatio) {
                // If contrast is still insufficient, fall back to black/white with maximum contrast
                textColor = if (luminance > 0.5) Color.Black else Color.White
            }

            return textColor
        }

        /**
         * Calculate luminance of a color (perceived brightness)
         * Uses the formula from WCAG 2.0
         */
        fun calculateLuminance(color: Color): Float {
            val rgb = colorToRGB(color)

            // Convert RGB to linear values
            val r = linearize(rgb.first / 255f)
            val g = linearize(rgb.second / 255f)
            val b = linearize(rgb.third / 255f)

            // Calculate luminance using WCAG formula
            return 0.2126f * r + 0.7152f * g + 0.0722f * b
        }

        /**
         * Convert RGB component to linear value
         */
        private fun linearize(colorComponent: Float): Float {
            return if (colorComponent <= 0.03928f) {
                colorComponent / 12.92f
            } else {
                ((colorComponent + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
            }
        }

        /**
         * Calculate contrast ratio between two colors
         * Based on WCAG 2.0 formula
         */
        private fun calculateContrastRatio(color1: Color, color2: Color): Float {
            val lum1 = calculateLuminance(color1)
            val lum2 = calculateLuminance(color2)

            val brightest = max(lum1, lum2)
            val darkest = min(lum1, lum2)

            return (brightest + 0.05f) / (darkest + 0.05f)
        }

        /**
         * Lighten a color by the given amount
         */
        fun lighten(color: Color, amount: Float): Color {
            val hsl = colorToHSV(color)
            hsl[2] = min(1f, hsl[2] + amount)
            return hsvToColor(hsl[0], hsl[1], hsl[2])
        }


        /**
         * Darken a color by the given amount
         */
        fun darken(color: Color, amount: Float): Color {
            val hsl = colorToHSV(color)
            hsl[2] = max(0f, hsl[2] - amount)
            return hsvToColor(hsl[0], hsl[1], hsl[2])
        }

        /**
         * Saturate a color by the given amount
         */
        fun saturate(color: Color, amount: Float): Color {
            val hsl = colorToHSV(color)
            hsl[1] = min(1f, hsl[1] + amount)
            return hsvToColor(hsl[0], hsl[1], hsl[2])
        }

        /**
         * Desaturate a color by the given amount
         */
        fun desaturate(color: Color, amount: Float): Color {
            val hsl = colorToHSV(color)
            hsl[1] = max(0f, hsl[1] - amount)
            return hsvToColor(hsl[0], hsl[1], hsl[2])
        }

        /**
         * Convert a Color to RGB components
         */
        private fun colorToRGB(color: Color): Triple<Int, Int, Int> {
            val argb = color.toArgb()
            return Triple(
                AndroidColor.red(argb),
                AndroidColor.green(argb),
                AndroidColor.blue(argb)
            )
        }

        /**
         * Convert a Color to HSV components
         */
        fun colorToHSV(color: Color): FloatArray {
            val argb = color.toArgb()
            val hsl = FloatArray(3)
            AndroidColor.RGBToHSV(
                AndroidColor.red(argb),
                AndroidColor.green(argb),
                AndroidColor.blue(argb),
                hsl
            )
            return hsl
        }

        private fun rgbToColor(r: Int, g: Int, b: Int): Color {
            return Color(AndroidColor.rgb(r, g, b))
        }

        /**
         * Create a Color from HSV components
         */
        private fun hsvToColor(hue: Float, saturation: Float, lightness: Float): Color {
            return Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, lightness)))
        }


        fun mixColors(color1: Color, color2: Color, ratio: Float): Color {
            val r1 = AndroidColor.red(color1.toArgb())
            val g1 = AndroidColor.green(color1.toArgb())
            val b1 = AndroidColor.blue(color1.toArgb())

            val r2 = AndroidColor.red(color2.toArgb())
            val g2 = AndroidColor.green(color2.toArgb())
            val b2 = AndroidColor.blue(color2.toArgb())

            val r = (r1 * (1 - ratio) + r2 * ratio).toInt()
            val g = (g1 * (1 - ratio) + g2 * ratio).toInt()
            val b = (b1 * (1 - ratio) + b2 * ratio).toInt()

            return rgbToColor(r, g, b)
        }

        fun getContrastingBlackOrWhite(color: Color): Color {
            // if luminance is greater than 0.5, return black else white
            return if (calculateLuminance(color) > 0.5) Color.Black else Color.White
        }

        fun colorToHex(color: Color): String {
            // Convert Color to ARGB hex string
            val argb = color.toArgb()
            return String.format("#%08X", argb)
        }

        fun hexToColor(hex: String): Color {
            // Parse hex string to Color
            return try {
                Color(AndroidColor.parseColor(hex))
            } catch (e: IllegalArgumentException) {
                Log.e("ColorUtils", "Invalid hex color: $hex", e)
                Color.Unspecified // Return unspecified color on error
            }
        }

        // endregion

        // region APP SPECIFIC COLOR FUNCTIONS

        private val streakColors: List<List<Color>> = listOf(
            // Newbie - Gray
            listOf(Color.Gray),
            // Pupil - Green
            listOf(Color.Green),
            // Specialist - Cyan
            listOf(Color.Cyan),
            // Expert - Blue
            listOf(Color.Blue),
            // Candidate Master - Violet
            listOf(Color(0xFFAA00AA)),
            // Master - Orange
            listOf(Color(0xFFFFAA00)),
            // Grandmaster - Red
            listOf(Color.Red),
            // Legendary Grandmaster - Black Red
            listOf(Color.Black, Color.Red),
            // tourist - Red Black
            listOf(Color.Red, Color.Black)
        )

        fun getStreakColors(streak: Int, desaturation: Float = 0.6f): List<Color> {
            // the streak colors are based on the codeforces color progression, iterated over the powers of 2
            // get the power of 2 at or below the streak
            val pow = min(if (streak > 0) floor(ln(streak.toDouble()) / ln(2.0)).toInt() else 0, streakColors.size - 1)
            return streakColors[pow].map { desaturate(it, desaturation) }
        }

        // endregion
    }
}

// region COLOR PICKER
// ? ........................

const val SATURATION = 0.7f
const val VALUE = 0.6f

@Composable
fun HuePicker(
    initialHue: Float,
    setColor: (Color) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val pressOffset = remember {
        mutableStateOf<Offset?>(null)
    }
    ComposeCanvas(
        modifier = Modifier
            .height(30.dp)
            .fillMaxWidth(0.95f)
//            .clip(RoundedCornerShape(10))
            .emitDragGesture(interactionSource)
    ) {
        if (pressOffset.value == null) {
            pressOffset.value = Offset((initialHue / 360f) * size.width, 0f)
        }
        val bitmapHeight = size.height * 2 / 3

        val drawScopeSize = size
        val bitmap = createBitmap(size.width.toInt(), bitmapHeight.toInt())
        val hueCanvas = Canvas(bitmap.asImageBitmap())
        val huePanel = RectF(0f, (size.height / 2f - bitmapHeight / 2f), bitmap.width.toFloat(), size.height / 2f + bitmapHeight / 2f)
        val hueColors = IntArray((huePanel.width()).toInt())
        var hue = 0f
        for (i in hueColors.indices) {
            hueColors[i] = AndroidColor.HSVToColor(floatArrayOf(hue, SATURATION, VALUE))
            hue += 360f / hueColors.size
        }
        val linePaint = Paint()
        linePaint.strokeWidth = 0F
        for (i in hueColors.indices) {
            linePaint.color = Color(hueColors[i])
            hueCanvas.drawLine(Offset(i.toFloat(), 0F), Offset(i.toFloat(), huePanel.bottom), linePaint)
        }
        drawBitmap(
            bitmap = bitmap,
            panel = huePanel,
            resources = context.resources,
            cornerRadius = 10.dp.toPx()
        )
        fun pointToHue(pointX: Float): Float {
            val width = huePanel.width()
            val x = when {
                pointX < huePanel.left -> 0F
                pointX > huePanel.right -> width
                else -> pointX - huePanel.left
            }
            return x * 360f / width
        }

        scope.collectForPress(interactionSource) { pressPosition ->
            val pressPos = pressPosition.x.coerceIn(0f..drawScopeSize.width)
            pressOffset.value = Offset(pressPos, 0f)
            val selectedHue = pointToHue(pressPos)
            setColor(
                Color(
                    AndroidColor.HSVToColor(floatArrayOf(selectedHue, SATURATION, VALUE))
                )
            )
        }

        drawCircle(
            Color.White,
            radius = size.height / 2 - 4,
            center = Offset(pressOffset.value?.x ?: 0f, size.height / 2),
            style = Stroke(
                width = 2.dp.toPx()
            )
        )
    }
}

fun CoroutineScope.collectForPress(
    interactionSource: InteractionSource,
    setOffset: (Offset) -> Unit
) {
    launch {
        interactionSource.interactions.collect { interaction ->
            (interaction as? PressInteraction.Press)
                ?.pressPosition
                ?.let(setOffset)
        }
    }
}

private fun Modifier.emitDragGesture(
    interactionSource: MutableInteractionSource
): Modifier = composed {
    val scope = rememberCoroutineScope()
    pointerInput(Unit) {
        detectDragGestures { input, _ ->
            scope.launch {
                interactionSource.emit(PressInteraction.Press(input.position))
            }
        }
    }.clickable(interactionSource, null) {
    }
}


private fun DrawScope.drawBitmap(
    bitmap: Bitmap,
    panel: RectF,
    resources: Resources,
    cornerRadius: Float = 10.dp.toPx(), // Default corner radius
) {
    val roundedBitmapDrawable: RoundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
        resources,
        bitmap
    )
    roundedBitmapDrawable.cornerRadius = cornerRadius

    drawIntoCanvas {
        roundedBitmapDrawable.setBounds(
            panel.left.toInt(),
            panel.top.toInt(),
            panel.right.toInt(),
            panel.bottom.toInt()
        )
        roundedBitmapDrawable.draw(it.nativeCanvas)
    }
}

// ? ........................
// endregion ........................
