package com.aritr.zinely.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.aritr.zinely.ui.theme.ZinelyDimens

/**
 * The AccessibleControl pattern of the parity plan (M1): spoken label + [Role.Button] + the frozen
 * ≥48dp touch target + keyboard focus ring — for custom-drawn pressables (shelf covers, bench
 * blocks) whose visual content is decoration, not text. The Z* buttons carry their own semantics
 * via their [androidx.compose.foundation.text.BasicText] children and do not need this.
 *
 * No haptic parameter by design: the frozen spec fires haptic verbs per *action* (Duplicate =
 * snap, Delete = boundary, Open = nothing), never per widget — callers own `haptics.perform(...)`
 * inside [onClick].
 */
@Composable
public fun Modifier.zinelyControl(
    label: String,
    enabled: Boolean = true,
    role: Role = Role.Button,
    focusRingRadius: Dp = ZinelyDimens.FocusRingRadius,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .zinelyFocusRing(interactionSource, focusRingRadius)
        .minimumInteractiveComponentSize()
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
        .semantics { contentDescription = label }
}
