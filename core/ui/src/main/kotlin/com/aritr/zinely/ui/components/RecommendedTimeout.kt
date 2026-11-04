package com.aritr.zinely.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager

/**
 * The longest "Time to take action" Android lets a person choose (Settings > Accessibility, API 29+): two
 * minutes. No phone setting asks for longer, so no bar of ours stays up longer than this (#160).
 */
public const val LONGEST_ACCESSIBILITY_TIMEOUT_MILLIS: Long = 120_000L

/**
 * How long a transient bar stays up: [baseMillis], or longer when the phone's accessibility "time to take
 * action" setting asks (W1 / #146), but **never forever** (#160). Every Shelf and Bench timer goes through here.
 *
 * Invariant: the result is at least [baseMillis] and at most [LONGEST_ACCESSIBILITY_TIMEOUT_MILLIS] (or
 * [baseMillis], if that is longer).
 *
 * Why the ceiling: before Android 10 there is no such setting, and Compose answers `Long.MAX_VALUE` for a bar
 * with a control whenever touch exploration (TalkBack) is on, copying the pre-Q Material Snackbar's
 * `LENGTH_INDEFINITE`. On the Shelf a bar that never goes holds the event collector: every later delete prompt
 * and notice waits behind it, and the delete it offers to undo is never written until the Shelf is left. Two
 * minutes is what a TalkBack user on Android 10+ can ask for at most, so the older phones get that.
 * [hasAction] adds the controls hint, which is what lets the platform return its longer value.
 */
public fun AccessibilityManager?.transientTimeoutMillis(baseMillis: Long, hasAction: Boolean): Long {
    val recommended = this?.calculateRecommendedTimeoutMillis(
        baseMillis,
        containsIcons = false,
        containsText = true,
        containsControls = hasAction,
    ) ?: baseMillis
    return recommended.coerceAtMost(maxOf(baseMillis, LONGEST_ACCESSIBILITY_TIMEOUT_MILLIS))
}

/** [transientTimeoutMillis] for the phone this composes on. Falls back to [baseMillis] when there is no manager. */
@Composable
internal fun recommendedTimeoutMillis(baseMillis: Long, hasAction: Boolean): Long =
    LocalAccessibilityManager.current.transientTimeoutMillis(baseMillis, hasAction)
