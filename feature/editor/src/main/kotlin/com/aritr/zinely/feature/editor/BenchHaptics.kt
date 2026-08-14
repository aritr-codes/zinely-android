package com.aritr.zinely.feature.editor

import androidx.compose.runtime.Composable
import com.aritr.zinely.ui.theme.ZinelyHaptic
import com.aritr.zinely.ui.theme.ZinelyTheme

/**
 * Wrap a Bench control's action so the hand is told it landed.
 *
 * Why the Bench needed this at all, what the audit counted, and why the choice is held here rather than
 * at forty-odd call sites: [ADR-102 §12.15](../../../../../../../../docs/DECISIONS.md#adr-102-bench-haptics).
 * The vocabulary is [ZinelyHaptic]'s — [Tick][ZinelyHaptic.Tick] for an ordinary action,
 * [Snap][ZinelyHaptic.Snap] for a selection landing, [Boundary][ZinelyHaptic.Boundary] for something that
 * pressing again does not undo.
 *
 * ⚠ **Call it inside the control, not at the screen that hosts it** — a Bench control is reached from the
 * pointer handler *and* from the accessibility `onClick` action, and one wrap here feeds both.
 *
 * ⚠ **Deliberately not `remember`ed**, so a caller that rebuilds its lambda cannot leave a stale one wired
 * to the finger. `remember(action)` would key on a lambda, which is the unstable case; `rememberUpdatedState`
 * fixes a staleness that cannot arise here.
 *
 * ⚠ **Bench-local by design.** The Library and the Proof perform at the *caller* instead, so a shared
 * component like `ZButton` must never perform one of its own. Inside-the-control is correct here only
 * because no Bench caller buzzes *for a Bench control*: `BenchStudioSurface` performs none at all, and
 * `EditorScreen`'s only haptics are `rememberStyleBuzz`, which belongs to the V1 `TypeBar` it also hosts.
 */
@Composable
internal fun benchTap(
    haptic: ZinelyHaptic = ZinelyHaptic.Tick,
    action: () -> Unit,
): () -> Unit {
    val haptics = ZinelyTheme.haptics
    return { haptics.perform(haptic); action() }
}
