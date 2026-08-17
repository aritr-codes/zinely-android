package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The **V2.1 handmade palette** — a one-to-one transcription of the `:root` custom properties in
 * `docs/design/mockups/v21-{library,proof,bench}.html`, specified by
 * [V21-SPEC.md](docs/design/V21-SPEC.md) and decided by [ADR-099](docs/DECISIONS.md#adr-099).
 *
 * Invariant, unchanged from V2: **the HTML is the specification.** A value may only change here
 * after it has changed in the corpus first. [ZinelyV21ContrastTest] pins every ratio, so drift in
 * either direction fails the build.
 *
 * ### Additive. [ZinelyV2Colors] and [ZinelyColors] are untouched
 *
 * ADR-099 supersedes the V2 trilogy **as the design source** and supersedes **no ADR**. The V2
 * palette stays exactly where it is until the screens that consume it are re-skinned, one surface at
 * a time. Nothing is migrated by this file.
 *
 * ### Every pairing here is measured, and the measurement changed the palette
 *
 * ADR-099 opened `Proposed` with no contrast evidence at all. Measuring it found **eight failing
 * pairings** ([V21-SPEC.md §4.1](docs/design/V21-SPEC.md)), five of which needed an owner ruling.
 * Two consequences are visible in this file and neither should be "tidied" away:
 *
 * - **[jamText] and [onJam] exist because [jam] cannot carry text.** `jam` on `paper` measures
 *   4.20:1 — under the 4.5 body floor. It keeps the fills, the cut line and the delete affordance;
 *   text and icons take `jamText`. This mirrors the [leaf] / [leafText] / [onLeaf] trio exactly, and
 *   that precedent is now the palette's **only** sanctioned route to a derived hue. A `berryText`
 *   added by analogy, without its own measurement, would be a misuse of it.
 * - **[inkFaint] sets no text.** It measures 3.04 / 3.40 on `paper` and the prototypes were using it
 *   for page numbers, captions and legend labels at 9.6–11.5px. All 26 of those moved to [inkSoft].
 *   The text ramp is **two tiers**, not three. `inkFaint` survives for decorative fills and strokes
 *   only, and [ZinelyV21ContrastTest] pins its ratio so that a future use as text fails loudly.
 *
 * ### A border is not a shadow ([inkLine])
 *
 * [inkLine] is the **hard shadow** colour and nothing else. In dark it is `#120E0A` — deliberately
 * near-black, because a printed shadow must stay darker than the paper it falls on. Every *drawn*
 * line (border, outline, SVG stroke) follows [ink] instead. Collapsing the two put **61 borders**
 * (8 Library · 19 Proof · 34 Bench) at 1.38:1 in dark and very nearly erased the Proof's fold guide;
 * they are byte-identical in light (`#33261C`), which is why the mistake survived a light-theme
 * reading. **56 `inkLine` uses remain and are correct:** 55 hard-shadow declarations plus one
 * deliberate border. See [V21-SPEC.md §4.3](docs/design/V21-SPEC.md).
 *
 * The exceptions are elements whose own ground **is** [ink], where an ink border would be invisible
 * and the shadow ink is correct: the Bench's snackbar and the Proof's flash toast — the same pattern,
 * resolved the same way. The rule is *a border contrasts with what it sits on*, not *a border is
 * always ink*.
 *
 * (A third ink-on-ink pair, the Bench's `.chip .sw`, is a colour **swatch** whose fill is the maker's
 * runtime ink; its ring separates it from the chip's `paper` ground and is correct as `ink`.)
 *
 * ### butter is material, never chrome
 *
 * [V21-SPEC.md §3.2](docs/design/V21-SPEC.md): butter carries **no action, no text, and no state
 * alone.** It is tape, stamps, the ring around the primary, the shelf lip, a caution ground. V2-TOKENS'
 * "no fourth chrome hue" rule survives V2.1 unchanged — butter joins the *material* side of the line
 * that rule already drew for cover inks. The one exception the spec used to record, a butter Undo on
 * the snackbar, was **retired** when it measured 1.59:1 in dark.
 *
 * ### Deliberately NOT ported — prototype scaffolding
 *
 * `--stage` is the backdrop *behind* the phone mock, not an in-app surface, and is excluded on the
 * same reasoning that excluded it from [ZinelyV2Colors]. `--voice` / `--serif` / `--sans` are type
 * roles and belong with typography. `--grain` is a texture, not a colour.
 */
@Immutable
public data class ZinelyV21Colors(
    // ----- the artifact and the room ---------------------------------------------------------
    /** `--paper` — the sheet you make on. */
    val paper: Color,
    /** `--paper-edge` — the sheet's cut edge, and page-stack edges. */
    val paperEdge: Color,
    /** `--desk` — the surface the paper lies on. */
    val desk: Color,
    /** `--desk-edge` — room dividers, the keyboard ground. */
    val deskEdge: Color,
    /** `--bench` — the Bench worktop; a darker desk, so the page reads as the hero. */
    val bench: Color,

    // ----- ink ---------------------------------------------------------------------------------
    /** `--ink` — body, headings, **and every drawn line**. ★ AA-critical on [paper]. */
    val ink: Color,
    /** `--ink-soft` — secondary text. ★ AA-critical on [paper]. */
    val inkSoft: Color,
    /**
     * `--ink-faint` — **decorative fills and strokes only; this token sets no text.**
     * 3.04 / 3.40 on [paper]. Its strokes on meaning-bearing graphics clear 1.4.11 by 0.04 and fail
     * on [bench] (2.30), which is a hard constraint on where those graphics may be drawn.
     */
    val inkFaint: Color,
    /** `--ink-line` — **the hard shadow only.** Never a border; see the class docs. */
    val inkLine: Color,

    // ----- the one action colour ---------------------------------------------------------------
    /** `--leaf` — the action colour as a fill. */
    val leaf: Color,
    /** `--leaf-text` — leaf as text or icon. ★ AA-critical on [paper]. */
    val leafText: Color,
    /** `--leaf-tint` — the action colour's quiet ground. */
    val leafTint: Color,
    /** `--on-leaf` — the label on a [leaf] fill. ★ AA-critical on [leaf]. */
    val onLeaf: Color,

    // ----- punctuation and material ------------------------------------------------------------
    /** `--berry` — punctuation: the current-page dot, stamps, the printer's-reach guide. */
    val berry: Color,
    /** `--berry-tint` — berry's ground. */
    val berryTint: Color,
    /** `--butter` — **material only**: tape, stamps, rings, the shelf lip. Never an action or state alone. */
    val butter: Color,
    /** `--butter-tint` — a caution ground. */
    val butterTint: Color,

    /**
     * `--on-butter` — the label on a [butter] fill. ★ AA-critical on [butter].
     *
     * **Added 2026-08-11 (ADR-100), and it is dark in *both* themes**, which is why it could not be
     * inferred from [onLeaf]. `butter` is a bright yellow at `#F6B22C` light and `#E8B458` dark — the one
     * hue the palette does not darken for the dark theme, because tape and stamps have to stay bright on
     * a dark desk. A cream label mirroring [onLeaf] would measure 1.6:1 on it. Dark ink measures **7.89**
     * and **9.15**.
     *
     * The dark figure read `9.99` here for one day, and it is worth recording why that survived: this
     * class's KDoc says [ZinelyV21ContrastTest] pins every ratio, so a wrong number *should* have failed
     * the build — but the pairing was never added to that file, and an unpinned number cannot be
     * contradicted by anything. It is pinned now. **A token documented as AA-critical and absent from the
     * contrast suite is the one shape of drift this palette has no defence against.**
     */
    val onButter: Color,

    // ----- consequence -------------------------------------------------------------------------
    /** `--jam` — the only urgent colour, **as fill and line**: delete, the cut line, error blocks. */
    val jam: Color,
    /** `--jam-text` — jam **as text or icon**. ★ AA-critical on [paper] and on [berryTint]. */
    val jamText: Color,
    /** `--on-jam` — the label on a [jamText] ground. ★ AA-critical on [jamText]. */
    val onJam: Color,

    // ----- translucent film --------------------------------------------------------------------
    /**
     * `--hair` — internal hairlines and dashed dividers. Translucent, so it is **not** measurable by
     * [WcagContrast] without compositing against its real backdrop first.
     */
    val hair: Color,
    /** `--shade` — the gutter shade beside a spine (Proof). Translucent. */
    val shade: Color,
    /** `--soft-shadow` — the soft contact shadow under a resting object. Translucent. */
    val softShadow: Color,
    /** `--contact` — the tighter contact shadow where an object meets its surface. Translucent. */
    val contact: Color,
)

/** The V2.1 palette in light theme — `:root` in all three prototypes. */
public fun zinelyV21LightColors(): ZinelyV21Colors = ZinelyV21Colors(
    paper = Color(0xFFFFF6E8),
    paperEdge = Color(0xFFEFDFC6),
    desk = Color(0xFFFBE9CE),
    deskEdge = Color(0xFFE9D2AE),
    bench = Color(0xFFEBD6B4),
    ink = Color(0xFF33261C),
    inkSoft = Color(0xFF6E5947),
    inkFaint = Color(0xFFA08B74),
    inkLine = Color(0xFF33261C),
    leaf = Color(0xFF4E7A3C),
    leafText = Color(0xFF3E6330),
    leafTint = Color(0xFFDCE8CE),
    onLeaf = Color(0xFFFFF6E8),
    berry = Color(0xFFE4879F),
    berryTint = Color(0xFFF8DCE2),
    butter = Color(0xFFF6B22C),
    butterTint = Color(0xFFFDEBC4),
    onButter = Color(0xFF33261C),
    jam = Color(0xFFCF4A28),
    jamText = Color(0xFFA63B20),
    onJam = Color(0xFFFFF6E8),
    hair = Color(0x2933261C),
    shade = Color(0x1233261C),
    softShadow = Color(0x4D4A3622),
    contact = Color(0x334A3622),
)

/**
 * The V2.1 palette in dark theme.
 *
 * Stated in full rather than as a diff from light, for the same reason [ZinelyV2Colors] does: this is
 * a re-derivation, not an inversion, and expressing it as overrides would misrepresent it. Note that
 * [ink] and [inkLine] — identical in light — **diverge here**, which is the whole point of keeping
 * them as two tokens.
 */
public fun zinelyV21DarkColors(): ZinelyV21Colors = ZinelyV21Colors(
    paper = Color(0xFF332B22),
    paperEdge = Color(0xFF3E3529),
    desk = Color(0xFF241E18),
    deskEdge = Color(0xFF2F2820),
    bench = Color(0xFF211B15),
    ink = Color(0xFFF6EAD6),
    inkSoft = Color(0xFFBFAC93),
    inkFaint = Color(0xFF8C7B65),
    inkLine = Color(0xFF120E0A),
    leaf = Color(0xFF8FAE6B),
    leafText = Color(0xFFAFC98C),
    leafTint = Color(0xFF333B27),
    onLeaf = Color(0xFF1E1A15),
    berry = Color(0xFFD3899B),
    berryTint = Color(0xFF3A2A2D),
    butter = Color(0xFFE8B458),
    butterTint = Color(0xFF3E3320),
    onButter = Color(0xFF1E1A15),
    jam = Color(0xFFE0755A),
    jamText = Color(0xFFE4856D),
    onJam = Color(0xFF1E1A15),
    hair = Color(0x26F6EAD6),
    shade = Color(0x38000000),
    softShadow = Color(0x9E000000),
    contact = Color(0x80000000),
)

/**
 * `.scrim{background:rgba(38,26,16,.44)}` — the one V2.1 scrim, for **every** surface that dims what is
 * behind it. `.44 × 255 = 112 = 0x70`.
 *
 * ⚠ **A literal, and not a token.** The rule sits outside `:root` in all three prototypes
 * (`v21-bench.html:359`, `v21-proof.html:323`, `v21-library.html:343` — the Library writes `.42`), so the
 * `prefers-color-scheme` block cannot reach it and V2.1 publishes no `--scrim` among the 25 the contrast
 * gate measured. Recorded as the same defect recurring rather than as a value anybody chose.
 *
 * ### Why it is here rather than in three files
 *
 * It was in three: `ZSheet.ScrimFill`, `BenchPageGrid.BenchGridScrimColor`, and V1's `colors.scrim` that
 * `ReframeOverlay` read. `BenchPageGrid`'s KDoc already said what to do about that — *"the scrim is shared
 * chrome … whoever converts `.sheet` should hoist this rather than write a second one"* — and a review
 * pointed out that the third caller had quietly borrowed the **grid's** constant instead: Reframe's dim is
 * not a modal backdrop but a permanent crop dimmer, never animated to full, and anyone darkening the modal
 * scrim so a sheet reads better over a busy bench would have silently changed how much of the user's
 * cropped-away photo stays visible. Same pixels, wrong reason, and nothing linking the two.
 *
 * So the name is purpose-neutral and the callers are listed rather than implied: **modal backdrops**
 * ([com.aritr.zinely.ui.components.ZSheet], `BenchPageGrid`) and **the Reframe crop dimmer**
 * (`ReframeOverlay`), which are different jobs wearing one value because the corpus declares one value.
 * A future surface that needs a *different* dim needs a different constant and a rule in the prototypes,
 * not an edit here.
 */
public val ZinelyV21Scrim: Color = Color(0x70261A10)
