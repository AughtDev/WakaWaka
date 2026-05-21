package com.aught.wakawaka.widget.aggregate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import androidx.core.graphics.toColorInt
import com.aught.wakawaka.data.CompletionTier
import com.aught.wakawaka.data.CompletionTierConfig

private data class BarKey(val tier: CompletionTier, val width: Int, val height: Int, val radius: Int)
private data class CrownKey(val tier: CompletionTier, val width: Int, val height: Int)

private val barCache = LruCache<BarKey, Bitmap>(32)
private val crownCache = LruCache<CrownKey, Bitmap>(16)

/**
 * Renders a rounded-rect bar in the tier color, overlaid with a single light diagonal sheen
 * stripe. The bitmap is masked through the tier rect via SRC_IN so the sheen never spills
 * outside the rounded corners.
 *
 * Sizes are in pixels; convert from dp at the call site using `density`.
 */
fun renderTierBar(tier: CompletionTier, widthPx: Int, heightPx: Int, radiusPx: Int): Bitmap {
    val w = widthPx.coerceAtLeast(1)
    val h = heightPx.coerceAtLeast(1)
    val r = radiusPx.coerceAtLeast(0).coerceAtMost(minOf(w, h) / 2)
    val key = BarKey(tier, w, h, r)
    barCache.get(key)?.let { return it }

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val tierColor = CompletionTierConfig.colors[tier]?.toColorInt() ?: Color.WHITE
    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    val layer = canvas.saveLayer(rect, null)

    // 1. Solid rounded rectangle in the tier color.
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tierColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, r.toFloat(), r.toFloat(), fill)

    // 2. Diagonal sheen stripe via gradient, masked to the rect we just drew so it stays
    //    inside the rounded corners.
    val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        shader = LinearGradient(
            w.toFloat(), 0f,
            0f, h.toFloat(),
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.argb((0.35f * 255).toInt(), 255, 255, 255),
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.42f, 0.5f, 0.58f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(rect, sheen)

    canvas.restoreToCount(layer)

    barCache.put(key, bitmap)
    return bitmap
}

/**
 * Renders the crown shape for the given tier to a transparent bitmap using the existing
 * `Canvas::drawXxxCrown` extensions from `screens/badges/canvas.kt`.
 */
fun renderTierCrown(tier: CompletionTier, widthPx: Int, heightPx: Int): Bitmap? {
    val drawFn = CompletionTierConfig.canvasCrown[tier] ?: return null
    val w = widthPx.coerceAtLeast(1)
    val h = heightPx.coerceAtLeast(1)
    val key = CrownKey(tier, w, h)
    crownCache.get(key)?.let { return it }

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val color = CompletionTierConfig.colors[tier]?.toColorInt() ?: Color.WHITE
    drawFn(canvas, color, w.toFloat(), h.toFloat())

    crownCache.put(key, bitmap)
    return bitmap
}
