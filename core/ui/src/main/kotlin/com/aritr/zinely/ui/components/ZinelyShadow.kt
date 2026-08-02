package com.aritr.zinely.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import com.aritr.zinely.ui.theme.ZinelyShadowLayer

/**
 * The multi-layer box-shadow of the DESIGN-FROZEN spec, deferred from M0 until its first caller
 * (ADR-048). Draws each [ZinelyShadowLayer] behind [shape]. CSS `box-shadow` has no x-offset
 * anywhere in the frozen corpus, so a layer is (dy, blur, color, spread). **Spread used to be
 * absent too** - the M0 audit verified that across the V1 trilogy and this comment carried it
 * forward as a fact; the V2 Bench's `.ctx` (`0 12px 30px -12px`) is the first layer that needs
 * it (ADR-092 row 2.10a). It defaults to zero, so every layer written before it draws as it did.
 *
 * CSS lists shadows front-to-back (the first declared layer paints on top), so layers are drawn in
 * reverse order here.
 *
 * Blur conversion: CSS `box-shadow` blur `b` is a Gaussian with sigma = b/2, while Android's
 * [BlurMaskFilter] maps its radius r to sigma = 0.57735·r + 0.5. Passing the frozen blur values
 * straight through would render visibly softer shadows and fail HTML-vs-device pixel parity, so the
 * conversion runs once here: r = (b/2 − 0.5) / 0.57735. A zero-blur layer (the `0 2px 0` hard paper
 * edge) is drawn as a crisp offset shape with no mask filter.
 */
public fun Modifier.zinelyShadow(layers: List<ZinelyShadowLayer>, shape: Shape): Modifier =
    drawBehind {
        for (layer in layers.asReversed()) drawShadowLayer(layer, shape)
    }

private fun DrawScope.drawShadowLayer(layer: ZinelyShadowLayer, shape: Shape) {
    // CSS spread grows the shadow shape by `spread` on every side before the blur (negative shrinks
    // it), so the outline is built at the spread size and re-centred. Corner radii are NOT spread with
    // it: the frozen -12px case leaves the shadow entirely inside the bar except for its dy, where a
    // radius difference on a 30px blur is not observable. Recorded rather than silently approximated.
    val spreadPx = layer.spread.toPx()
    val spreadSize = androidx.compose.ui.geometry.Size(
        (size.width + 2f * spreadPx).coerceAtLeast(0f),
        (size.height + 2f * spreadPx).coerceAtLeast(0f),
    )
    val outline = shape.createOutline(spreadSize, layoutDirection, this)
    val path = Path().apply {
        when (outline) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
    val blurPx = layer.blur.toPx()
    translate(left = -spreadPx, top = layer.dy.toPx() - spreadPx) {
        if (blurPx <= 0f) {
            drawPath(path, layer.color)
        } else {
            val paint = Paint().also { it.color = layer.color }
            paint.asFrameworkPaint().maskFilter =
                BlurMaskFilter(cssBlurToAndroidRadius(blurPx), BlurMaskFilter.Blur.NORMAL)
            drawIntoCanvas { it.drawPath(path, paint) }
        }
    }
}

/** r = (sigma − 0.5) / 0.57735 with sigma = cssBlur / 2; clamped so tiny blurs stay drawable. */
internal fun cssBlurToAndroidRadius(cssBlurPx: Float): Float =
    (((cssBlurPx / 2f) - 0.5f) / 0.57735f).coerceAtLeast(0.1f)
