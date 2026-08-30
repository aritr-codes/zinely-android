package com.aritr.zinely.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer

/**
 * Draws a list of [ZinelyV2ShadowLayer] behind [shape] — the V2 half of [zinelyShadow], landing with
 * its first caller exactly as [ZinelyV2ShadowLayer]'s own KDoc said it would ("the modifier that draws
 * them lands with the first component that needs it (Phase B)"). That caller is the Library's Maker's
 * Cover, whose two-layer `contact` + `shadow` pair is what makes a cover read as an object resting on
 * the desk rather than a rectangle floating above it.
 *
 * **Why this is not just [zinelyShadow] with one more field.** V1's frozen spec never used `spread`;
 * V2 uses it in 20 of its 25 chrome shadows and *never positively* — the `0 Ypx Bpx -Spx` idiom that
 * pulls a soft shadow back under its object. Spread is therefore not decoration here, it is what stops
 * a 24px blur from haloing. CSS spreads a shadow by insetting the shadow's box by `-spread` on every
 * side **and** growing each corner radius by the same amount (CSS Backgrounds §7.1.1), which is why
 * this cannot be approximated by scaling the path about its centre: a uniform scale would move the
 * radii proportionally instead of additively, and the corners would be visibly wrong at the cover's
 * asymmetric 6/9/9/6 radius.
 *
 * The CSS-blur → [BlurMaskFilter]-radius conversion is shared with V1's [zinelyShadow] via
 * [cssBlurToAndroidRadius] rather than restated, so the two paths cannot drift apart.
 *
 * CSS lists shadows front-to-back (the first declared layer paints on top), so layers draw in reverse.
 * `inset` layers are **not** handled here and deliberately so — an inset `0 0 0 1px` in this corpus is
 * a 1px inner hairline, which in Compose is a border inside the bounds, not a shadow
 * ([ZinelyV2ShadowLayer] KDoc, idiom 1).
 */
public fun Modifier.zinelyV2Shadow(layers: List<ZinelyV2ShadowLayer>, shape: Shape): Modifier =
    drawWithCache {
        // A sheet animates for many draw frames without changing its size, shape, or theme. Building its
        // Path, Paint, and BlurMaskFilter on every one of those frames made the first Art opening pay the
        // same immutable setup cost repeatedly. CacheDrawScope invalidates this block whenever any input
        // read here changes, while onDrawBehind reuses the prepared objects for the animation itself.
        val outline = shape.createOutline(size, layoutDirection, this)
        val prepared = layers.asReversed().map { layer ->
            val path = spreadPath(outline, layer.spread.toPx())
            val blurPx = layer.blur.toPx()
            PreparedV2Shadow(
                path = path,
                color = layer.color,
                dyPx = layer.dy.toPx(),
                paint = if (blurPx <= 0f) {
                    null
                } else {
                    Paint().also { paint ->
                        paint.color = layer.color
                        paint.asFrameworkPaint().maskFilter =
                            BlurMaskFilter(cssBlurToAndroidRadius(blurPx), BlurMaskFilter.Blur.NORMAL)
                    }
                },
            )
        }
        onDrawBehind {
            prepared.forEach { shadow ->
                translate(top = shadow.dyPx) {
                    val paint = shadow.paint
                    if (paint == null) {
                        drawPath(shadow.path, shadow.color)
                    } else {
                        drawIntoCanvas { it.drawPath(shadow.path, paint) }
                    }
                }
            }
        }
    }

private data class PreparedV2Shadow(
    val path: Path,
    val color: Color,
    val dyPx: Float,
    val paint: Paint?,
)

/**
 * The shadow's own outline: [outline] inflated by [spread] on every side, corner radii grown by the
 * same amount (floored at zero — a corner cannot curve inward).
 *
 * A [Outline.Generic] shape has no radii to grow and no meaningful uniform inflation, so a non-zero
 * spread on one is rejected rather than silently dropped: dropping it would draw a halo where the
 * design asked for a contact shadow, and nothing in a screenshot would say why.
 */
internal fun spreadPath(outline: Outline, spread: Float): Path = Path().apply {
    when (outline) {
        is Outline.Rectangle -> {
            val clamped = clampSpread(spread, outline.rect.width, outline.rect.height)
            addRect(outline.rect.inflate(clamped))
        }

        is Outline.Rounded -> {
            val clamped = clampSpread(spread, outline.roundRect.width, outline.roundRect.height)
            addRoundRect(outline.roundRect.inflate(clamped))
        }

        is Outline.Generic -> {
            require(spread == 0f) {
                "zinelyV2Shadow cannot spread a Generic outline: there are no corner radii to grow. " +
                    "Use a Rect/RoundedCornerShape shape, or state the spread as 0."
            }
            addPath(outline.path)
        }
    }
}

/** A spread negative enough to overlap the box's own centre would invert left/right or top/bottom. */
private fun clampSpread(spread: Float, width: Float, height: Float): Float =
    spread.coerceAtLeast(-minOf(width, height) / 2f)

/** CSS spread on a rounded box: the box grows by [spread], and so does every non-zero corner radius. */
private fun RoundRect.inflate(spread: Float): RoundRect = RoundRect(
    left = left - spread,
    top = top - spread,
    right = right + spread,
    bottom = bottom + spread,
    topLeftCornerRadius = topLeftCornerRadius.grow(spread),
    topRightCornerRadius = topRightCornerRadius.grow(spread),
    bottomRightCornerRadius = bottomRightCornerRadius.grow(spread),
    bottomLeftCornerRadius = bottomLeftCornerRadius.grow(spread),
)

/**
 * CSS Backgrounds §7.1.1: a **sharp** corner (zero radius) stays sharp under spread — only a corner that
 * is already rounded grows. Growing every corner unconditionally would round a square box's corners the
 * moment it got a shadow, which nothing in this codebase's callers currently exercises but the spec does
 * not allow either.
 */
private fun CornerRadius.grow(spread: Float): CornerRadius =
    if (x <= 0f && y <= 0f) {
        this
    } else {
        CornerRadius((x + spread).coerceAtLeast(0f), (y + spread).coerceAtLeast(0f))
    }
