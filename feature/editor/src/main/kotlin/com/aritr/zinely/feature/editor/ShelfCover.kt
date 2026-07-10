package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.components.ZPaperSurface
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyColors
import com.aritr.zinely.ui.theme.ZinelyEasing
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.delay
import kotlin.math.min

/** `.zine:nth-child(3n+k) .cover{ --tilt }` — the shelf is set by hand, so nothing hangs plumb. */
internal fun shelfTilt(index: Int): Float = when (index % 3) {
    0 -> -1.1f
    1 -> 0.8f
    else -> -0.4f
}

/**
 * `.zine::after` — the ledge the object rests on. A hairline that overhangs the object by 6% on each
 * side, 9dp up from the bottom of the `.zine` box.
 *
 * Apply this **before** the `.zine` bottom padding (`Modifier.shelfLedge().padding(bottom = 16.dp)`),
 * so the draw scope sees the padded box the CSS `::after` is positioned against.
 */
@Composable
internal fun Modifier.shelfLedge(): Modifier {
    val line = ZinelyTheme.colors.shelfLine
    return drawBehind {
        val thickness = 1.dp.toPx()
        drawRect(
            color = line,
            topLeft = Offset(-0.06f * size.width, size.height - 9.dp.toPx() - thickness),
            size = Size(1.12f * size.width, thickness),
        )
    }
}

/**
 * `@keyframes settle` + `.shelf.arrive .zine` — objects are set onto their ledges in a calm stagger:
 * 12dp up, .38s on the frozen ease, `36ms * min(index, 10)` apart. The cap is the spec's own
 * (`--i:${Math.min(i,10)}`): a shelf of forty zines must not take a second and a half to arrive.
 *
 * The spec's `both` fill means the object is invisible *before* its delay elapses, not merely at t=0.
 * Under reduced motion the animation collapses to nothing — the object is simply already there.
 */
@Composable
internal fun Modifier.shelfSettle(index: Int): Modifier {
    if (LocalZinelyMotion.current.reduceMotion) return this
    val settle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(min(index, 10) * 36L)
        settle.animateTo(1f, tween(durationMillis = 380, easing = ZinelyEasing))
    }
    return graphicsLayer {
        alpha = settle.value
        translationY = (1f - settle.value) * 12.dp.toPx()
    }
}

/**
 * `.cover` — a zine as a small physical object: a 3:4 sheet of paper, printed.
 *
 * The [recipe] decides the ink, the archetype and the paper stock; [index] decides the tilt. Content
 * (kicker, title, edition) is the caller's, laid out inside the frozen `15px 14px` padding and over
 * the ink — the spec keeps text at `z-index:2` on paper so it stays legible whatever the band does.
 *
 * Deliberately absent, and unchanged from the M2 deviations already recorded: the `.wip` variant
 * (`.cover.wip .art{opacity:.5}` and the pencil tick) — no `ProjectRepository` field says a zine is
 * unfinished, so nothing can drive it. The hover/focus lift and the `:active` press are the *card's*
 * to own, since they read a click's interaction source; they land with it, not here.
 */
@Composable
internal fun ShelfCover(
    recipe: ShelfCoverRecipe,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = ZinelyTheme.colors
    val ink1 = recipe.field.toColor(colors)
    // `var(--ink2, transparent)` everywhere except Split, whose `::after` falls back to `var(--teal)`.
    val ink2 = recipe.accent?.toColor(colors)
        ?: colors.teal.takeIf { recipe.archetype == ShelfArchetype.Split }

    ZPaperSurface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .graphicsLayer {
                rotationZ = shelfTilt(index)
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 3.dp),
        boundEdgeWidth = 7.dp,
        boundEdgeAlpha = 0.14f,
        usePaper2 = recipe.usePaper2,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCoverStock()
            drawInkBand(recipe.archetype, ink1, ink2)
            drawFold(colors.inkFaint)
        }
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 15.dp), content = content)
    }
}

private fun ShelfInk.toColor(colors: ZinelyColors): Color = when (this) {
    ShelfInk.Coral -> colors.coral
    ShelfInk.Teal -> colors.teal
    ShelfInk.Stamp -> colors.stamp
    ShelfInk.Yellow -> colors.yellow
}

/**
 * `.cover{ background-image: … }` — the spine tooth and the top-left corner shade that keep the sheet
 * from reading as flat fill. The CSS radial is `120% 90% at 0% 0%`; on a 3:4 sheet `1.2w` and `0.9h`
 * are the same length, so one circular gradient is the ellipse, not an approximation of it.
 */
private fun DrawScope.drawCoverStock() {
    drawRect(color = Color.Black.copy(alpha = 0.05f), size = Size(2.dp.toPx(), size.height))
    drawRect(
        brush = Brush.radialGradient(
            0.0f to Color.Black.copy(alpha = 0.035f),
            0.46f to Color.Transparent,
            1.0f to Color.Transparent,
            center = Offset.Zero,
            radius = 1.2f * size.width,
        ),
    )
}

/**
 * `.cover .art` — the loud part: a riso ink band bleeding across the middle third of the object.
 *
 * The band is `top:32%; height:47%`, clipped, and blended `multiply` **as a whole**: the second ink
 * overprints the first normally (that is what `::after` over `::before` does inside the element), and
 * only the composite multiplies onto the paper. That is why this saves a layer instead of passing
 * `BlendMode.Multiply` to each `drawRect` — per-shape multiply would darken ink-over-ink twice.
 *
 * Both inks are painted on a box inset `-24% -12%` beyond the band, so the print bleeds off every
 * edge rather than sitting inside a visible rectangle. The `::after` is nudged `3px, 4px` off
 * register — the misprint is the point.
 */
private fun DrawScope.drawInkBand(archetype: ShelfArchetype, ink1: Color, ink2: Color?) {
    val bandTop = 0.32f * size.height
    val bandHeight = 0.47f * size.height
    val bandWidth = size.width

    drawIntoCanvas { canvas ->
        canvas.saveLayer(
            bounds = Rect(0f, bandTop, bandWidth, bandTop + bandHeight),
            paint = Paint().apply { blendMode = BlendMode.Multiply },
        )
        clipRect(left = 0f, top = bandTop, right = bandWidth, bottom = bandTop + bandHeight) {
            translate(top = bandTop) {
                val bleed = Rect(
                    left = -0.12f * bandWidth,
                    top = -0.24f * bandHeight,
                    right = 1.12f * bandWidth,
                    bottom = 1.24f * bandHeight,
                )
                when (archetype) {
                    ShelfArchetype.Sun -> {
                        // `inset:-46% 15%` + `border-radius:50%` — a sun, not a bleed rectangle.
                        val disc = Rect(
                            left = 0.15f * bandWidth,
                            top = -0.46f * bandHeight,
                            right = 0.85f * bandWidth,
                            bottom = 1.46f * bandHeight,
                        )
                        drawOval(ink1, disc.topLeft, disc.size, alpha = 0.92f)
                        if (ink2 != null) {
                            translate(3.dp.toPx(), 4.dp.toPx()) {
                                drawOval(ink2, disc.topLeft, disc.size, alpha = 0.5f)
                            }
                        }
                    }

                    ShelfArchetype.Bars -> {
                        drawBars(bleed, ink1, alpha = 0.92f)
                        if (ink2 != null) {
                            translate(3.dp.toPx(), 4.dp.toPx()) { drawBars(bleed, ink2, alpha = 0.5f) }
                        }
                    }

                    // `.a-tone .art::after{ display:none }` — halftone is a single ink, always.
                    ShelfArchetype.Tone -> drawHalftone(bleed, ink1, alpha = 0.92f)

                    ShelfArchetype.Split -> {
                        // Two clipped fields meeting on a diagonal; the `::after` keeps its register.
                        drawPath(bleed.polygon(0f to 0f, 1f to 0f, 1f to 1f, 0.42f to 1f), ink1, alpha = 0.92f)
                        if (ink2 != null) {
                            drawPath(bleed.polygon(0f to 0f, 0.42f to 0f, 0f to 1f), ink2, alpha = 0.85f)
                        }
                    }
                }
            }
        }
        canvas.restore()
    }
}

/** `repeating-linear-gradient(180deg, ink 0 9px, transparent 9px 19px)`. */
private fun DrawScope.drawBars(bleed: Rect, ink: Color, alpha: Float) {
    val on = 9.dp.toPx()
    val period = 19.dp.toPx()
    var y = bleed.top
    while (y < bleed.bottom) {
        drawRect(
            color = ink,
            topLeft = Offset(bleed.left, y),
            size = Size(bleed.width, min(on, bleed.bottom - y)),
            alpha = alpha,
        )
        y += period
    }
}

/**
 * `radial-gradient(ink 30%, transparent 32%)` tiled `9px 9px`.
 *
 * The gradient's implicit farthest-corner radius on a 9×9 tile is `√(4.5² + 4.5²) ≈ 6.36px`, so the
 * 30%→32% stop pair is a hard-edged dot of radius ≈1.97px. Compose antialiases the circle, which is
 * what the browser's 2%-wide ramp does anyway.
 */
private fun DrawScope.drawHalftone(bleed: Rect, ink: Color, alpha: Float) {
    val tile = 9.dp.toPx()
    val radius = 0.31f * tile * 1.4142f / 2f
    var y = bleed.top + tile / 2f
    while (y < bleed.bottom) {
        var x = bleed.left + tile / 2f
        while (x < bleed.right) {
            drawCircle(ink, radius = radius, center = Offset(x, y), alpha = alpha)
            x += tile
        }
        y += tile
    }
}

/** A `clip-path:polygon(…)` in fractions of [this] rectangle. */
private fun Rect.polygon(vararg points: Pair<Float, Float>): Path = Path().apply {
    points.forEachIndexed { i, (fx, fy) ->
        val x = left + fx * width
        val y = top + fy * height
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/**
 * `.cover .fold` — the one signature gesture, whispered on every object: a scored crease down the
 * centre, `3px` on and `5px` off, from 9% to 91% of the sheet.
 */
private fun DrawScope.drawFold(inkFaint: Color) {
    val thickness = 1.dp.toPx()
    val x = size.width / 2f - thickness / 2f
    val top = 0.09f * size.height
    val bottom = 0.91f * size.height
    val dash = 3.dp.toPx()
    val period = 8.dp.toPx()
    var y = top
    while (y < bottom) {
        drawRect(
            color = inkFaint.copy(alpha = 0.26f),
            topLeft = Offset(x, y),
            size = Size(thickness, min(dash, bottom - y)),
        )
        y += period
    }
}
