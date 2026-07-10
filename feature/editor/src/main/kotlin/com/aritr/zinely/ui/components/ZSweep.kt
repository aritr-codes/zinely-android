package com.aritr.zinely.ui.components

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aritr.zinely.ui.theme.ZinelyEasing
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * The frozen loading shimmer: a `linear-gradient(100deg, transparent 20%, rgba(255,255,255,.35)
 * 50%, transparent 80%)` band sweeping `translateX(-100%) → 100%` at 1.5s, frozen ease, infinite.
 * The host shape differs per surface (shelf skeleton covers, bench load page, proof load sheet) —
 * hence a modifier, not a component; apply after the host's background, inside its clip.
 *
 * Reduced motion, per the frozen spec, does NOT remove the overlay — it freezes it in place at
 * `opacity:.4, transform:none` (a static wash), so reduced-motion goldens keep the loading look.
 */
@Composable
public fun Modifier.zinelySweep(): Modifier {
    val reduceMotion = ZinelyTheme.motion.reduceMotion
    if (reduceMotion) {
        return drawWithContent {
            drawContent()
            drawRect(brush = sweepBrush(size.width, size.height), alpha = 0.4f)
        }
    }
    val transition = rememberInfiniteTransition(label = "zinelySweep")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 1_500, easing = ZinelyEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "zinelySweepProgress",
    )
    return drawWithContent {
        drawContent()
        // translateX(-100%) → translateX(100%): shift the gradient axis, not the rect.
        drawRect(brush = sweepBrush(size.width, size.height, progress * size.width))
    }
}

/** CSS 100deg ≈ 10° off horizontal: the band leans slightly, running left→right. */
private fun sweepBrush(width: Float, height: Float, dx: Float = 0f): Brush {
    val lean = height * 0.176f // tan(10°) ≈ 0.176 — the gradient axis tilt of `100deg`
    return Brush.linearGradient(
        colorStops = arrayOf(
            0.2f to Color.Transparent,
            0.5f to Color.White.copy(alpha = 0.35f),
            0.8f to Color.Transparent,
        ),
        start = Offset(dx, lean),
        end = Offset(dx + width, -lean),
    )
}
