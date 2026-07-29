package com.aritr.zinely.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * `--settle: cubic-bezier(.05,.7,.1,1)` — the **arrival** curve.
 *
 * Almost all of its ease is spent at the end: it leaves fast and lands slowly, which is how a sheet
 * of paper set down on a desk behaves.
 *
 * **The axis is paper versus chrome**, and it holds for all nine frozen uses: the page pan, the
 * `mat` materialise, the page thumbnails (which the frozen source calls *"tiny printed pages resting
 * in a row"*), the keyboard stack, the bottom sheet, the page grid, the Proof's page transform, the
 * drawer, and the `seal`. Every one of them moves a **paper-metaphor object**. Reach for
 * [ZinelyV2Standard] for anything that is chrome mechanism or pure opacity, even when it visibly
 * moves — the frozen tray height and toggle thumb both do.
 */
public val ZinelyV2Settle: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/**
 * `--standard: cubic-bezier(.2,0,0,1)` — the **everything else** curve.
 *
 * Symmetric-in, hard-out. It is the **chrome** half of the paper/chrome split described on
 * [ZinelyV2Settle], and it covers eight frozen uses: the autosave flash, the tray height, the context
 * bar, the ink popover, the toggle thumb, the snackbar, the scrim, and the Proof page's *opacity*
 * (whose transform, in the same declaration at `v2-proof.html:100`, takes settle — the split stated
 * in one line).
 *
 * Note that two of those eight — the tray resizing and the toggle thumb sliding — move physically.
 * The axis is not "moves versus informs"; it is what kind of *object* is moving.
 */
public val ZinelyV2Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * The V2 motion contract — **two easings and a reduced-motion policy, and deliberately no durations.**
 *
 * ### Why there are no duration tokens
 *
 * V1's [ZinelyMotion] carries `fast` (130ms) and `base` (230ms) because the V1 spec tokenised
 * `--fast` and `--base`. **V2 tokenises neither.** The Bench and Proof each declare exactly two motion
 * properties — `--settle` and `--standard`, both easings — and the Library declares none at all
 * (**D-011**). Every duration is written at its use site: **sixteen** distinct values — .1, .12, .13,
 * .14, .15, .16, .18, .2, .22, .24, .25, .28, .3, .34 and .4s, plus the caret's 1.05s — on no ladder.
 *
 * That is the same shape as V2's spacing, radius and type, and it is now settled the same way: the
 * owner ruling closing **D-007** directs that where the design corpus defines no shared scale, none
 * is introduced and values live at the component. So durations are transcribed per component from
 * Phase B onward, and this class holds only what the design actually shares.
 *
 * ### The reduced-motion policy, and the one distinction that matters
 *
 * All three frozen files carry a `prefers-reduced-motion` block, and **all three write a different
 * rule** (logged as **D-012**):
 *
 * | File | Rule |
 * |---|---|
 * | `v2-library.html:138` | `*{transition:none!important}` |
 * | `v2-bench.html:260` | `*{transition-duration:.01ms!important; animation:none!important}` |
 * | `v2-proof.html:246` | `*{transition-duration:.01ms!important; animation-duration:.01ms!important}` |
 *
 * For the animations that exist today the three land in the same place, which is why the divergence
 * survived the freeze. But they are not interchangeable, and this class adopts the **Bench's** rule:
 * `animation:none`, not a collapsed duration. The Bench is the only file with a *looping* animation —
 * the text caret's `blink 1.05s steps(1) infinite` — and collapsing an infinite animation's duration
 * rather than disabling it does not calm it, it makes it strobe. A reduced-motion preference is in
 * part a photosensitivity setting, so that is the failure that actually hurts.
 *
 * Adopting one of three written rules **is a choice**, and it is worth being plain about which way it
 * cuts: the files were frozen at 09:08, 13:52 and 15:53, so the Bench's rule is the *older* of the two
 * live statements and the Proof's — the corpus's most recent word — is the one rejected. See
 * **D-012**, where that ordering is disclosed and a ruling requested. What justifies choosing here
 * rather than stopping is that one option is a safety floor rather than a preference, and that the
 * choice is free to reverse while this API has no callers.
 *
 * This class therefore encodes the distinction the CSS only implies: one-shot motion **collapses to
 * zero** ("arrive instantly, still arrive"), and continuous motion **does not run at all**.
 */
@Immutable
public data class ZinelyV2Motion(val reduceMotion: Boolean) {

    /**
     * Whether a **looping or indefinitely repeating** animation may run at all.
     *
     * Distinct from a zero duration on purpose: a one-shot at 0ms is an instant arrival, whereas a
     * repeating animation at 0ms is an unbounded loop with no delay. The frozen caret blink is the
     * live example. Gate every `infiniteRepeatable`, marquee, pulse or shimmer on this.
     *
     * **Nothing enforces it yet**, and that is a known weakness rather than an oversight: a forgotten
     * check is caught by no test, lint or type here. The fix is a `rememberV2InfiniteTransition`-style
     * wrapper that reads the local itself and cannot be bypassed — landed at the **first Phase B call
     * site**, when there is something to wrap. Building it now would be a guard for a caller that does
     * not exist, and Compose's repeating primitive is a composable rather than a spec, so it cannot be
     * neutralised by returning a different value from this class.
     */
    public val allowsContinuousMotion: Boolean = !reduceMotion

    /**
     * Resolve a frozen duration against the preference: the component's own value, or **0** when the
     * user asked for reduced motion.
     *
     * The spec's `.01ms` is expressible in Compose only as 0, which is the same intent without the
     * fractional millisecond — the same reading V1 took.
     */
    public fun durationMillis(frozenMillis: Int): Int = if (reduceMotion) 0 else frozenMillis

    /**
     * A `<duration> var(--settle)` tween — for motion that comes to rest.
     *
     * The duration is the caller's because the design gives every component its own; the *easing* and
     * the reduced-motion collapse are shared, which is precisely what this class exists to hold.
     */
    public fun <T> settle(frozenMillis: Int): TweenSpec<T> =
        tween(durationMillis = durationMillis(frozenMillis), easing = ZinelyV2Settle)

    /** A `<duration> var(--standard)` tween — for motion that merely informs. */
    public fun <T> standard(frozenMillis: Int): TweenSpec<T> =
        tween(durationMillis = durationMillis(frozenMillis), easing = ZinelyV2Standard)
}
