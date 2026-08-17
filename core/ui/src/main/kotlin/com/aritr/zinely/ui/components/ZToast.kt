package com.aritr.zinely.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import kotlinx.coroutines.delay

/** `toastTimer=setTimeout(..., 2200)` — identical in all three frozen files. */
internal const val ZINELY_TOAST_TIMEOUT_MILLIS: Long = 2_200L

/**
 * The frozen `.flash` — a passive confirmation with no action. 2.2s flat timer, component-owned,
 * restarted when [message] changes. `role=status` → polite live region.
 *
 * Positioning is the caller's: bottom-centre at the surface's frozen offset.
 *
 * ### V2.1 — ADR-102 P8
 *
 * ```css
 * .flash{transform:translateX(-50%) rotate(-1deg);background:var(--ink);color:var(--paper);
 *   font-size:.78rem;font-weight:500;padding:var(--gap-sm) var(--gap-lg);
 *   border-radius:var(--br-pill);opacity:0;transition:opacity .2s;border:1.5px solid var(--ink-line)}
 * .flash.on{opacity:1}
 * ```
 *
 * `v21-proof.html .flash` is the only actionless toast in the three prototypes, so it is the class
 * this component transcribes. Three V2 marks are deleted rather than converted:
 *
 * - **the blurred `0 12px 30px rgba(0,0,0,.4)` shadow** — `.flash` declares none, and no printed
 *   shadow replaces it;
 * - **the 20px entrance slide** — `.flash` transitions `opacity` and nothing else. Its resting
 *   `rotate(-1deg)` is not part of the entrance either; it is held in both states;
 * - **the ink/desk inversion** — V2 swapped two theme roles to get a dark bar. V2.1 names the two
 *   colours outright, `--ink` under `--paper`.
 *
 * The border is `--ink-line`, not `--ink`, and that inversion of this package's usual rule is
 * deliberate: the file says so in the rule itself, because this toast's own ground **is** `--ink` and
 * an ink border on it would be invisible. Same pattern and same resolution as the Bench's
 * [ZSnackbar]; the frozen comment adds *"the two must not diverge"*.
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
    val colors = ZinelyTheme.v21Colors
    val motion = ZinelyTheme.motion
    val entered = remember { MutableTransitionState(false) }.apply { targetState = true }
    AnimatedVisibility(
        visibleState = entered,
        modifier = modifier,
        enter = fadeIn(motion.fast()),
    ) {
        BasicText(
            text = message,
            modifier = Modifier
                .graphicsLayer { rotationZ = Tilt }
                .clip(ToastShape)
                .background(colors.ink)
                .border(ToastBorder, colors.inkLine, ToastShape)
                // padding:var(--gap-sm) var(--gap-lg)
                .padding(
                    horizontal = ZinelyV21Dimens.gapLg,
                    vertical = ZinelyV21Dimens.gapSm,
                )
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = TextStyle(
                color = colors.paper,
                fontFamily = ZinelyV21Fonts.Work,
                fontSize = MessageSize,
                fontWeight = FontWeight.Medium,
                lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-proof.html .flash`.
// ---------------------------------------------------------------------------------------------

/** `border-radius:var(--br-pill)`. */
private val ToastShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)

/** `border:1.5px solid var(--ink-line)` — ink-line **deliberately**; see the KDoc. */
private val ToastBorder = 1.5.dp

/** `rotate(-1deg)`, held in both states. The Bench's `.snack` tilts `-.6deg`; these are two objects. */
private const val Tilt = -1f

/** `.flash{font-size:.78rem;font-weight:500}` = 12.48px. */
private val MessageSize = 12.48.sp
