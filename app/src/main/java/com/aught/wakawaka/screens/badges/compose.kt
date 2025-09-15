package com.aught.wakawaka.screens.badges

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aught.wakawaka.utils.ColorUtils

// region CROWNS
// ? ........................

fun DrawScope.goldCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadraticTo(w / 2, sz + 1 + yOff, w - sw, sw + yOff)
        lineTo(w - sw, sz + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        ),
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(sw, -sw * 1 + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw * 2 + yOff),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w - sw, -sw * 1 + yOff),
        style = Fill
    )

}

fun DrawScope.bronzeCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw * 3 / 2f, sz * 3 / 4f + yOff)
        quadraticTo(w / 2, sz * 3 / 4f + yOff, w / 2, sw + yOff)
        quadraticTo(w / 2, sz * 3 / 4f + yOff, w - sw * 3 / 2f, sz * 3 / 4f + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw / 2),
        style = Fill
    )
}

fun DrawScope.silverCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw * 3 / 2f, sz * 3 / 4f + yOff)
        lineTo(sw * 3 / 2f, sw + yOff)
        quadraticTo(w / 2, sz + 1 + yOff, w - sw * 3 / 2f, sw + yOff)
        lineTo(w - sw * 3 / 2f, sz * 3 / 4f + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw + yOff),
        style = Fill
    )
}

fun DrawScope.diamondCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadraticTo(w / 4, sz + yOff, w / 2, 0f + yOff)
        quadraticTo(3 * w / 4, sz + yOff, w - sw, sw + yOff)
        lineTo(w - sw, sz + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w - sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
}

fun DrawScope.royalPurpleCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw, sz + yOff)
        lineTo(sw, sw + yOff)
        quadraticTo(w / 4, sz + yOff, w / 2, 0f + yOff)
        quadraticTo(3 * w / 4, sz + yOff, w - sw, sw + yOff)
        lineTo(w - sw, sz + yOff)
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w / 2, -sw * 3f + yOff),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw,
        center = Offset(w - sw, -sw * 4 / 3f + yOff),
        style = Fill
    )
}

fun DrawScope.royalRedCrown(color: Color, size: Int) {
    val w = this.size.width
    val sz = size.toFloat()
    val sw = sz / 5
    val yOff = sz / 5
    val crownPath = Path().apply {
        moveTo(sw / 2, sz + yOff)
        lineTo(sw / 2, sw * 2 + yOff)
        quadraticTo(w / 8, sz + yOff, w / 4, sw * 3 / 2 + yOff)
        quadraticTo(w / 2, sz + yOff, 3 * w / 4, sw + yOff)
        quadraticTo(7 * w / 8, sz + yOff, w - sw / 2, sw * 3 / 2 + yOff)
        lineTo(w - sw / 2, sz + yOff)
    }
    val gemPath = Path().apply {
        moveTo(w / 2 - sw, -sz / 2 + yOff)
        lineTo(w / 2, -sz * 4 / 5f + yOff)
        lineTo(w / 2 + sw, -sz / 2 + yOff)
        lineTo(w / 2, -sz * 1 / 5f + yOff)
        close()
    }
    drawPath(
        path = crownPath,
        color = color.copy(alpha = 0.4f),
        style = Stroke(
            width = sw,
            cap = StrokeCap.Round
        )
    )
    drawPath(
        path = gemPath,
        color = color.copy(alpha = 0.7f),
        style = Stroke(
            width = sw * 2 / 3f,
            cap = StrokeCap.Round
        )
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw * 2 / 3f,
        center = Offset(sw, -sw + yOff),
        style = Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = sw,
        center = Offset(w / 4, -sw * 4 / 3f + yOff),
        style = Stroke(
            width = sw * 2 / 3f,
            cap = StrokeCap.Round
        )
//        style=Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = sw,
        center = Offset(3 * w / 4, -sw * 4 / 3f + yOff),
        style = Stroke(
            width = sw * 2 / 3f,
            cap = StrokeCap.Round
        )
//                style=Fill
    )
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = sw * 2 / 3f,
        center = Offset(w - sw, -sw + yOff),
        style = Fill
    )
}


// ? ........................
// endregion ........................

@Composable
fun BadgeText(
    hours: Int,
    color: Color,
    fontSize: Int,
    animated: Boolean,
    drawBehind: DrawScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sheen_transition")

    val sheenTranslate = if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3500,
                    // with random delay to avoid all badges shining at the same time
                    delayMillis = 500,
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "sheen_translate"
        ).value
    } else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.shiny(color, sheenTranslate)
//        modifier = Modifier.drawBehind(drawBehind)
    ) {
        Text(
            text = "$hours",
            color = color.copy(alpha = 0.7f),
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.8f).sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 8.dp)
                .drawBehind(drawBehind)
        )
        Text(
            text = "h",
            color = color.copy(alpha = 0.5f),
            fontSize = (fontSize * 2 / 3).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = 5.dp, x = 0.dp)
        )
    }
}

fun DrawScope.mainUnderline(color: Color, size: Int) {
    val offset = size * 2 / 3f
    drawLine(
        color.copy(alpha = 0.3f),
        start = Offset(
            x = 10f,
            y = this.size.height - offset
        ),
        end = Offset(
            x = this.size.width - 10f,
            y = this.size.height - offset
        ),
        strokeWidth = size / 3f,
        cap = StrokeCap.Round
    )
}


fun Modifier.shiny(badgeColor: Color, translate: Float) = this.drawWithCache {
    // The sheen will be a diagonal gradient
    val sheenBrush = Brush.linearGradient(
        colors = listOf(
            badgeColor.copy(0.5f),
            badgeColor.copy(0.5f),
            badgeColor.copy(0.4f),
            badgeColor,
            badgeColor.copy(0.4f),
            badgeColor.copy(0.5f),
            badgeColor.copy(0.5f),
        ),
        start = Offset(0f + size.width * translate, 0f),
        end = Offset(size.width * 2 + size.width * translate, size.height / 2)
    )


    val paint = Paint()

    val layerBounds = Rect(Offset.Zero, size)

    onDrawWithContent {
        // 1. Draw the original content (the badge) into a layer
        drawIntoCanvas { canvas ->
            canvas.saveLayer(layerBounds, paint)
            // This is where the original @Composable content goes
            drawContent()

            val sheenPaint = Paint().apply {
                this.blendMode = BlendMode.SrcIn
            }

            canvas.saveLayer(layerBounds, sheenPaint)

            // 2. Draw the sheen on top, using SrcIn to mask it
            // to the badge's shape.
            drawRect(
                brush = sheenBrush,
            )

            canvas.restore()

            canvas.restore()
        }
    }
}


@Composable
fun HourCountBadge(hours: Int, size: Int = 14, animated: Boolean = false) {
    Box() {
        if (hours < 25) null
        else {
            val milestoneIndex = getMilestoneIndex(hours)
            val milestone = MILESTONES[milestoneIndex]
            val col = ColorUtils.hexToColor(milestone.colorHex)
            BadgeText(milestone.hours, col, size, animated) {
                mainUnderline(col, size)
                milestone.crown(this, col, size)
            }
        }
    }
}
