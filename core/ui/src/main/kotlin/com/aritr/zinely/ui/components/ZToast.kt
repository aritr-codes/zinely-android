package com.aritr.zinely.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyShadowLayer
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.delay

/** `toastTimer=setTimeout(..., 2200)` — identical in all three frozen files. */
internal const val ZINELY_TOAST_TIMEOUT_MILLIS: Long = 2_200L

/**
 * The frozen `.toast` — a passive, inverted (ink-on-desk swapped) confirmation with no action.
 * 2.2s flat timer, component-owned, restarted when [message] changes. `role=status` → polite live
 * region. Centered text + width cap are proof.html additions that are invisible on the one-line
 * strings the other surfaces show — applied unconditionally.
 *
 * Positioning is the caller's: bottom-centre at the surface's frozen offset (96dp shelf,
 * 104dp bench/proof).
 */
@Composable
public fun ZToast(
    message: String,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        delay(ZINELY_TOAST_TIMEOUT_MILLIS)
        onTimeout()
    }
    val colors = ZinelyTheme.colors
    val motion = ZinelyTheme.motion
    val entered = remember { MutableTransitionState(false) }.apply { targetState = true }
    val enterOffsetPx = with(LocalDensity.current) { 20.dp.roundToPx() }
    AnimatedVisibility(
        visibleState = entered,
        modifier = modifier,
        enter = fadeIn(motion.fast()) + slideInVertically(motion.fast()) { enterOffsetPx },
    ) {
        val shape = RoundedCornerShape(12.dp)
        BasicText(
            text = message,
            modifier = Modifier
                .zinelyShadow(
                    listOf(ZinelyShadowLayer(dy = 12.dp, blur = 30.dp, color = Color.Black.copy(alpha = 0.4f))),
                    shape,
                )
                .clip(shape)
                .background(colors.onDesk)
                // padding:11px 16px
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = TextStyle(
                color = colors.desk,
                fontFamily = ZinelyTheme.typography.shell,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
