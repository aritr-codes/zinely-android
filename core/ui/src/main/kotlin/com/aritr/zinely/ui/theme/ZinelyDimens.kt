package com.aritr.zinely.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The dimensions the frozen spec states **globally**, and nothing else.
 *
 * There is deliberately no spacing scale and no radius scale here. The DESIGN-FROZEN trilogy does
 * not define one: its `gap` and `border-radius` values are chosen per component (`border-radius`
 * alone takes sixteen distinct values across the three files, on no ladder). Inventing a scale would
 * put a second, competing source of truth next to the HTML — exactly what the Documentation Rule and
 * the HTML-first workflow forbid. Components carry their own frozen values from M1 onward.
 */
public object ZinelyDimens {
    /**
     * Minimum interactive target. The spec sets `min-height:48px` on its controls, which is also the
     * Android accessibility floor — the two agree, so there is one number.
     */
    public val MinTouchTarget: Dp = 48.dp

    /**
     * The focus indicator, from `:focus-visible{ outline:3px solid var(--coral-strong);
     * outline-offset:2px; border-radius:6px; }`. Draw it in `ZinelyColors.coralStrong`.
     */
    public val FocusRingWidth: Dp = 3.dp
    public val FocusRingOffset: Dp = 2.dp
    public val FocusRingRadius: Dp = 6.dp
}
