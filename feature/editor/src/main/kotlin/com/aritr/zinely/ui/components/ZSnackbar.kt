package com.aritr.zinely.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.delay

/** `undoTimer=setTimeout(hideSnack, 5000)` — identical in all three frozen files. */
internal const val ZINELY_SNACKBAR_TIMEOUT_MILLIS: Long = 5_000L

/**
 * The frozen `.snackbar` — undo-over-confirm (destructive = undo, never a dialog): stamp fill,
 * yellow bold action, 5s flat auto-dismiss. The timer is component-owned, restarted when [message]
 * changes, and deliberately does NOT pause while the action is focused — the spec records that as
 * an accepted limitation (proof.html RI-4); pausing would be a post-freeze interaction addition.
 *
 * On show, focus moves to the action button (the control that triggered the snackbar was destroyed
 * by the re-render in the spec; same rationale holds for a removed list item). `role=status` maps
 * to a polite live region — the double announcement (live region + focus) is spec-true.
 *
 * Positioning is the caller's: align bottom-centre with the surface's frozen offset
 * (96dp shelf, 104dp bench/proof).
 */
@Composable
public fun ZSnackbar(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        delay(ZINELY_SNACKBAR_TIMEOUT_MILLIS)
        onTimeout()
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(message) { focusRequester.requestFocus() }

    val colors = ZinelyTheme.colors
    val motion = ZinelyTheme.motion
    // .on: opacity 0->1, translateY(20px)->0 at --fast
    val entered = remember { MutableTransitionState(false) }.apply { targetState = true }
    val enterOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { 20.dp.roundToPx() }
    AnimatedVisibility(
        visibleState = entered,
        modifier = modifier,
        enter = fadeIn(motion.fast()) + slideInVertically(motion.fast()) { enterOffsetPx },
    ) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .zinelyShadow(
                    listOf(ZinelyShadowLayer(dy = 12.dp, blur = 30.dp, color = Color.Black.copy(alpha = 0.4f))),
                    shape,
                )
                .clip(shape)
                .background(colors.stamp)
                // padding:4px 6px 4px 16px
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 6.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = message,
                style = TextStyle(
                    color = Color(0xFFF4EFE6),
                    fontFamily = ZinelyTheme.typography.shell,
                    fontSize = 14.sp,
                ),
            )
            val actionInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .zinelyFocusRing(actionInteraction, 10.dp)
                    .focusRequester(focusRequester)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = actionInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onAction,
                    )
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = actionLabel,
                    style = TextStyle(
                        color = colors.yellow,
                        fontFamily = ZinelyTheme.typography.shell,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.02.em,
                    ),
                )
            }
        }
    }
}
