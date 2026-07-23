package com.aritr.zinely.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * `--ease: cubic-bezier(.2,.7,.3,1)` — the single frozen easing curve. Calm shell; the one signature
 * motion (the fold) lives in the Proof surface, not here.
 */
public val ZinelyEasing: Easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1.0f)

/** `--fast: 130ms`. */
public const val ZINELY_FAST_MILLIS: Int = 130

/** `--base: 230ms`. */
public const val ZINELY_BASE_MILLIS: Int = 230

/**
 * The frozen motion contract, resolved against the user's reduced-motion preference.
 *
 * The spec collapses every transition to `.001ms` under `@media (prefers-reduced-motion: reduce)`,
 * i.e. "arrive instantly, still arrive". We use 0ms, which is the same thing without the fractional
 * millisecond that Compose cannot express.
 *
 * Read it from [LocalZinelyMotion]; never hardcode a duration at a call site.
 */
@Immutable
public data class ZinelyMotion(val reduceMotion: Boolean) {
    /** `--fast`, or 0 when the user asked for reduced motion. */
    public val fastMillis: Int = if (reduceMotion) 0 else ZINELY_FAST_MILLIS

    /** `--base`, or 0 when the user asked for reduced motion. */
    public val baseMillis: Int = if (reduceMotion) 0 else ZINELY_BASE_MILLIS

    /** A `--fast var(--ease)` tween. */
    public fun <T> fast(): TweenSpec<T> = tween(durationMillis = fastMillis, easing = ZinelyEasing)

    /** A `--base var(--ease)` tween. */
    public fun <T> base(): TweenSpec<T> = tween(durationMillis = baseMillis, easing = ZinelyEasing)
}
