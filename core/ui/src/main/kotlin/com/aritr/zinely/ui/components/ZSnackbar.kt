package com.aritr.zinely.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import kotlinx.coroutines.delay

/**
 * The undo window. **5000ms is V2's value, and the claim that it was "identical in all three frozen
 * files" was false even of V2's corpus — a P8 review caught it.**
 *
 * `v21-bench.html:763` is the only snackbar timer in the V2.1 trilogy and it writes **3200**:
 * `tt=setTimeout(()=>$('snack').classList.remove('show'),3200)`. This constant is deliberately **not**
 * changed here. P8 converts what these components paint, and an undo window is not paint: shortening
 * the time a user has to recover a destructive action by 36% is a product decision with an
 * accessibility cost, on three surfaces at once, and it belongs to the owner rather than to a re-skin.
 * Recorded as an open item for ADR-102 rather than fixed silently or left un-noticed.
 */
internal const val ZINELY_SNACKBAR_TIMEOUT_MILLIS: Long = 5_000L

/**
 * The frozen `.snack` — undo-over-confirm (destructive = undo, never a dialog): 5s flat auto-dismiss.
 *
 * The timer is component-owned, restarted when [message] changes, and deliberately does NOT pause
 * while the action is focused — the spec records that as an accepted limitation (proof.html RI-4);
 * pausing would be a post-freeze interaction addition.
 *
 * On show, focus moves to the action button (the control that triggered the snackbar was destroyed
 * by the re-render in the spec; same rationale holds for a removed list item). `role=status` maps
 * to a polite live region — the double announcement (live region + focus) is spec-true.
 *
 * Positioning is the caller's: align bottom-centre with the surface's frozen offset.
 *
 * ### V2.1 — ADR-102 P8
 *
 * ```css
 * .snack{background:var(--ink);color:var(--paper);border:1.5px solid var(--ink-line);
 *   border-radius:var(--br-pill);padding:var(--gap-md) var(--gap-sm) var(--gap-md) var(--gap-lg);
 *   font-size:.79rem;transform:translateY(8px) rotate(-.6deg);transition:opacity .18s,transform .18s}
 * .snack.show{transform:rotate(-.6deg)}
 * .snack button{font-size:.78rem;font-weight:700;color:var(--paper);text-decoration:underline;
 *   text-underline-offset:3px;text-decoration-thickness:1.5px;background:none;border:0;
 *   border-radius:var(--br-pill);padding:var(--gap-xs) var(--gap-md)}
 * ```
 *
 * **Three things the re-freeze changed and this file must not smooth over.**
 *
 * 1. **The blurred `0 12px 30px rgba(0,0,0,.4)` drop shadow is gone**, and nothing replaces it. The
 *    bar has no printed shadow either — it is the only V2.1 surface that carries neither, which the
 *    tilt is doing instead.
 * 2. **The border is `--ink-line`, not `--ink`**, and that is the one place in this package where
 *    those two are not the usual "line = ink, shadow = inkLine" pairing. The frozen file explains it
 *    in the rule itself: this element's own ground **is** `--ink`, so an ink border would be
 *    invisible. The rule is *"a border contrasts with what it sits on"*.
 * 3. **The action is no longer butter.** `--butter` measured 7.89:1 in light but **1.59:1 in dark**,
 *    where the ink ground turns cream. It is now the bar's own `--paper`, underlined so it still
 *    reads as the action — and the underline is therefore load-bearing, not decoration.
 *
 * The bar takes no press tier: `.snack` has no `:active` rule, and its button has neither border nor
 * shadow to shed.
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

    val colors = ZinelyTheme.v21Colors
    val motion = ZinelyTheme.motion
    // `.snack.show`: opacity 0->1 and translateY(8px)->0, both at .18s.
    val entered = remember { MutableTransitionState(false) }.apply { targetState = true }
    val enterOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { EnterRise.roundToPx() }
    AnimatedVisibility(
        visibleState = entered,
        modifier = modifier,
        enter = fadeIn(motion.fast()) + slideInVertically(motion.fast()) { enterOffsetPx },
    ) {
        Row(
            modifier = Modifier
                // `transform:… rotate(-.6deg)` — held in both states, so it is the bar's resting
                // attitude rather than part of the entrance.
                .graphicsLayer { rotationZ = Tilt }
                .clip(SnackShape)
                .background(colors.ink)
                .border(SnackBorder, colors.inkLine, SnackShape)
                // padding:var(--gap-md) var(--gap-sm) var(--gap-md) var(--gap-lg)
                .padding(
                    start = ZinelyV21Dimens.gapLg,
                    top = ZinelyV21Dimens.gapMd,
                    bottom = ZinelyV21Dimens.gapMd,
                    end = ZinelyV21Dimens.gapSm,
                )
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(ZinelyV21Dimens.gapMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = message,
                style = TextStyle(
                    color = colors.paper,
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = MessageSize,
                    lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                ),
            )
            val actionInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .zinelyFocusRing(actionInteraction, ZinelyV21Dimens.radiusPill)
                    .focusRequester(focusRequester)
                    .clip(RoundedCornerShape(ZinelyV21Dimens.radiusPill))
                    .clickable(
                        interactionSource = actionInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onAction,
                    )
                    // The frozen `padding:var(--gap-xs) var(--gap-md)` is below the touch minimum, so
                    // the 48dp floor stands — the same split `.dclose` makes between drawn and
                    // reachable.
                    .defaultMinSize(minHeight = ActionTouchTarget)
                    .padding(horizontal = ZinelyV21Dimens.gapMd),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = actionLabel,
                    style = TextStyle(
                        color = colors.paper,
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = ActionSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = ZinelyV21Fonts.InheritedLineHeight,
                        // The underline is the action's only remaining distinction from the message
                        // now that both are `--paper`. Compose has no analogue for the frozen
                        // `text-underline-offset:3px` / `text-decoration-thickness:1.5px`, so the
                        // platform's own underline metrics stand — recorded, not silently matched.
                        //
                        // ⚠ **This line is a device-verification item, and it cannot be otherwise.** A
                        // review correctly flagged that nothing asserted it — it is the only thing left
                        // saying "this is the button" once `butter` was retired for its 1.59:1 dark
                        // contrast, so a later tidy-up could delete it as decoration. But it is
                        // unassertable here twice over: `TextDecoration` lives in a `BasicText`'s
                        // `style`, never in its semantics, so no `onNodeWith…` can read it; and a raster
                        // probe finds nothing either, because Robolectric's headless text stack draws the
                        // glyphs and **not the decoration** (measured: widest contiguous run inside the
                        // action's own bounds, 6px across a 55px box, with the underline present and
                        // correct). An assertion written against that would have been a probe defect
                        // dressed as a finding — the fourth of this package, all in the same direction.
                        // Discharged on hardware instead — measurements and reading in ADR-102 §12.13
                        // ("The second batch"), not restated here. A test that cannot fail for the right
                        // reason is worse than an honest device gate.
                        textDecoration = TextDecoration.Underline,
                    ),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The frozen values, transcribed from `v21-bench.html .snack`.
// ---------------------------------------------------------------------------------------------

/** `border-radius:var(--br-pill)`. */
private val SnackShape: Shape = RoundedCornerShape(ZinelyV21Dimens.radiusPill)

/** `border:1.5px solid var(--ink-line)` — ink-line **deliberately**; see the KDoc. */
private val SnackBorder = 1.5.dp

/** `transform:translateY(8px)` on the hidden state. */
private val EnterRise = 8.dp

/** `rotate(-.6deg)`, held in both states. */
private const val Tilt = -0.6f

/** `.snack{font-size:.79rem}` = 12.64px. */
private val MessageSize = 12.64.sp

/** `.snack button{font-size:.78rem;font-weight:700}` = 12.48px. */
private val ActionSize = 12.48.sp

/** Not frozen: the touch minimum the drawn padding falls short of. */
private val ActionTouchTarget = 48.dp
