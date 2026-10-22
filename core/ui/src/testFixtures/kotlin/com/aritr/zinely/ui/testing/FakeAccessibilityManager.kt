package com.aritr.zinely.ui.testing

import androidx.compose.ui.platform.AccessibilityManager

/**
 * A stand-in for the system's accessibility timeout setting ("time to take action"). Provide it with
 * `CompositionLocalProvider(LocalAccessibilityManager provides FakeAccessibilityManager(20_000L))`.
 * Shared by `:core:ui` and `:feature:editor` tests (W1 R-W9).
 */
public class FakeAccessibilityManager(private val recommendedMillis: Long) : AccessibilityManager {
    override fun calculateRecommendedTimeoutMillis(
        originalTimeoutMillis: Long,
        containsIcons: Boolean,
        containsText: Boolean,
        containsControls: Boolean,
    ): Long = maxOf(originalTimeoutMillis, recommendedMillis)
}
