package com.aritr.zinely.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalAccessibilityManager

/**
 * The time a transient bar stays up: the frozen [baseMillis], or longer when the phone's accessibility
 * "time to take action" setting asks (W1 / #146). Both bars carry text; [hasAction] adds the controls hint,
 * which is what lets the system return its longer value. Falls back to [baseMillis] when there is no manager.
 */
@Composable
internal fun recommendedTimeoutMillis(baseMillis: Long, hasAction: Boolean): Long =
    LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        baseMillis,
        containsIcons = false,
        containsText = true,
        containsControls = hasAction,
    ) ?: baseMillis
