package com.aritr.zinely.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The **V2.1 geometry scales** — and unlike V2, there genuinely are some.
 *
 * [ZinelyV2Dimens] carries no scale of any kind, and that was correct: **D-006** and **D-007** were
 * findings *about the frozen V2 corpus*, which had 16 distinct chrome radii and 16.7% on-grid
 * spacing. Publishing a scale over that would have been fabrication. **Neither ruling is overturned
 * here** ([ADR-099 §2.4](docs/DECISIONS.md#adr-099)); the corpus changed, not the principle.
 *
 * The two halves of that claim did not arrive equally, and the difference is recorded because it
 * changes how much each scale can be trusted:
 *
 * - **The radius scale was authored.** `--br-*` is declared in all three prototypes and used **104
 *   times** (23 Library · 35 Proof · 46 Bench) with `--hard` 24 times and `--frame` 6. It earns
 *   publication on exactly the evidence ADR-099 claimed for it.
 * - **The spacing scale was refitted.** V21-SPEC originally said the corpus was *"authored with a
 *   scale from the first line"*. For spacing that was **false**: 281 declarations across 30 distinct
 *   values, a near-continuous ramp from 1 to 18px, with `--gap-*` declared in one prototype and used
 *   in none. An independent review caught it. On the owner's ruling the corpus was **moved to match
 *   the claim** — 273 declarations routed through the ladder below, mean movement 0.94px, 97.1%
 *   within 2px — rather than the claim withdrawn. That is a weaker provenance than the radius scale
 *   has, and a future reader should know which of the two is a discovery and which is a decision.
 *
 * See [V21-SPEC.md §3.3](docs/design/V21-SPEC.md) and
 * [V21-RESEARCH.md §8.3](docs/design/V21-RESEARCH.md).
 */
public object ZinelyV21Dimens {

    /**
     * The radius ladder — `--br-*`, straight from the corpus.
     *
     * Pills for controls, [radiusMd] / [radiusLg] for cards and drawers, [radiusXl] for a bottom
     * sheet's top corners.
     */
    public val radiusXs: Dp = 4.dp
    public val radiusSm: Dp = 8.dp
    public val radiusMd: Dp = 14.dp
    public val radiusLg: Dp = 22.dp
    public val radiusXl: Dp = 36.dp

    /** `--br-pill: 999px` — a full pill. Compose expresses this as a percent shape at the call site. */
    public val radiusPill: Dp = 999.dp

    /**
     * The spacing ladder — `--gap-*`, Maeve's `4/8/16/24/36` plus `12` where the corpus clustered and
     * a `2px` hairline step it could not do without.
     *
     * Values above 40px in the corpus are one-off layout dimensions rather than rhythm (60, 64, 132,
     * 150) and are deliberately **not** on the ladder; they stay at their call sites.
     *
     * ### The 8pt question, asked and closed — the ladder is whole
     *
     * [V2-CONSTITUTION §III](docs/design/V2-CONSTITUTION.md) requires an **8pt rhythm for layout**, and
     * [gapHair], [gapXs], [gapMd] and [gap2Xl] are off that grid. It took three rulings to settle, and
     * the middle one is the cautionary part: this file briefly split the ladder into "layout" and
     * "sub-component" steps to reconcile them, and a review found [gapMd] spacing a page grid, the
     * canvas region, three panel types and two page-level states across the frozen corpus — ~59
     * declarations, the most-used step there is. **The split was invented to save a conflict, and it
     * did not survive contact with the corpus.**
     *
     * The owner then closed the question underneath it: **the rhythm is a generosity principle, not a
     * modular grid.** [D-007](docs/DECISIONS.md) measured the V2 trilogy — ratified under that same
     * constitution — at **16.7% on-grid**; a clause its own design violated five times in six was never
     * operating as a grid. What it governs is what it says: *spacing is calm and generous; density is
     * never used to fit more chrome.*
     *
     * So every step below is usable anywhere, and the rule for callers is the ordinary one:
     * **transcribe the corpus value at its frozen address.** Do not reach for [gapLg] where the HTML
     * says 12 — the ladder needs no defending, and moving a pixel away from the specification to make
     * a surface look grid-clean is the thing Principle 11 forbids.
     */
    public val gapHair: Dp = 2.dp
    public val gapXs: Dp = 4.dp
    public val gapSm: Dp = 8.dp
    public val gapMd: Dp = 12.dp
    public val gapLg: Dp = 16.dp
    public val gapXl: Dp = 24.dp
    public val gap2Xl: Dp = 36.dp

    /**
     * `--hard: 4px` — the signature move. An offset shadow in `inkLine`, **zero blur**, always
     * down-right. It is a *printed* shadow, not elevation: the object physically moves under the
     * finger.
     *
     * This is the **hero** offset and the only one the corpus tokenises. Two more depths are used as
     * literals, and the three yield **four** press behaviours — [ZinelyV21Press.Hero],
     * [ZinelyV21Press.Raised], [ZinelyV21Press.Flat] and [ZinelyV21Press.Inline], which shares Flat's
     * rest depth but halves the travel.
     */
    public val hardShadow: Dp = 4.dp

    /**
     * `--frame: 5px` — a flat colour ring stacked outside a hard shadow, borrowed from Maeve as riso
     * misregistration. **Reserved for the one primary action on a screen** (Save PDF, Add).
     */
    public val frameRing: Dp = 5.dp

    /**
     * `.icon-btn:disabled{opacity:.35}` (`v21-bench.html:345`) — **the corpus's one disabled treatment**,
     * and the reason it is a token rather than a literal at each site.
     *
     * The freeze declares `:disabled` on exactly one selector, so every other disabled control in the app
     * is *borrowing* this number rather than transcribing its own rule. It lived as
     * `BenchContextBarDisabledAlpha` in `BenchContextBar.kt` and was read across the file boundary by
     * `BenchStyleRow` — one surface importing another surface's private constant, which is how a shared
     * value ends up owned by whichever file happened to need it first.
     *
     * ⚠ It is an **opacity**, and how it is applied is not part of the token. `.icon-btn` fades whole
     * (ground, border and glyph together), which is what `graphicsLayer { alpha = }` gives; but a control
     * whose child colour is load-bearing — the ink swatch on `BenchStyleRow`'s chip, which row 3.9 requires
     * to be the element's *true* ink — must pre-multiply instead, or the layer alpha falsifies it. Same
     * number, two applications, and the choice belongs to the caller.
     */
    public const val disabledAlpha: Float = 0.35f
}
