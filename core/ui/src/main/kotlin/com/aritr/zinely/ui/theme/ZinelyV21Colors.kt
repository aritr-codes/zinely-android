package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The **V2.1 handmade palette** — a one-to-one transcription of the `:root` custom properties in
 * `docs/design/mockups/v21-{library,proof,bench}.html`, specified by
 * [V21-SPEC.md](docs/design/V21-SPEC.md) and decided by [ADR-099](docs/DECISIONS.md#adr-099).
 *
 * Invariant, unchanged from V2: **the HTML is the specification.** A value may only change here
 * after it has changed in the corpus first. [ZinelyV21ContrastTest] pins the usage-driven pairings
 * that the shipped UI forms, so meaningful contrast drift fails the build.
 *
 * **2026-08-24 palette amendment:** [THEME-37596-FREEZE.md](docs/design/THEME-37596-FREEZE.md)
 * supersedes the earlier colour values. Its six labelled swatches are now exact identity colours.
 * [paper] and [paperEdge] are theme-invariant physical stock; [surface] and [surfaceSoft] carry app
 * chrome. The dark theme changes the studio around the zine, never the zine itself.
 *
 * ### Additive. [ZinelyV2Colors] and [ZinelyColors] are untouched
 *
 * ADR-099 supersedes the V2 trilogy **as the design source** and supersedes **no ADR**. The V2
 * palette stays exactly where it is until the screens that consume it are re-skinned, one surface at
 * a time. Nothing is migrated by this file.
 *
 * ### Every pairing here is measured, and the measurement changed the palette
 *
 * The palette is validated against the pairings the UI actually forms, not against isolated swatches.
 * Two consequences are visible in this file and neither should be "tidied" away:
 *
 * - **[jamText] and [onJam] keep consequence text separate from consequence fills.** Their actual
 *   surface pairings are pinned by [ZinelyV21ContrastTest].
 * - **[inkFaint] sets no text.** It is the exact Matcha swatch used only for decorative fills and
 *   strokes. Secondary text belongs to [inkSoft].
 *
 * ### A border is not a shadow ([inkLine])
 *
 * [inkLine] is the **hard shadow** colour and nothing else. In light it follows the source palette's
 * lit ink (`#27270F`); in dark it remains near-black (`#120E0A`) so a printed shadow stays darker
 * than the object it grounds. Every drawn line (border, outline, SVG stroke) follows [ink] instead.
 * See [V21-SPEC.md §4.3](docs/design/V21-SPEC.md).
 *
 * (The Bench's `.chip .sw` is a colour **swatch** whose fill is the maker's
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
    /** `--surface` — raised app chrome. Never use this for a zine page or print preview. */
    val surface: Color,
    /** `--surface-soft` — supporting chrome and gentle status grounds. */
    val surfaceSoft: Color,

    // ----- ink ---------------------------------------------------------------------------------
    /** `--ink` — body, headings, **and every drawn line**. ★ AA-critical on [paper]. */
    val ink: Color,
    /** `--ink-soft` — secondary text. ★ AA-critical on [paper]. */
    val inkSoft: Color,
    /**
     * `--ink-faint` — **decorative fills and strokes only; this token sets no text.**
     * It is the source image's exact Matcha swatch; meaning-bearing graphics use a ground-aware ink.
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
     * Wild Primrose (`#E9E29B`) stays bright in both themes because it represents a physical material.
     * Its label therefore uses dark ink in both themes rather than inheriting the surrounding chrome's
     * light dark-theme copy. [ZinelyV21ContrastTest] pins this shipped pairing.
     */
    val onButter: Color,

    // ----- consequence -------------------------------------------------------------------------
    /** `--jam` — the only urgent colour, **as fill and line**: delete, the cut line, error blocks. */
    val jam: Color,
    /** `--jam-text` — the consequence hue as text/icon on the dark/light chrome surfaces. */
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
    desk = Color(0xFFE9E29B),
    deskEdge = Color(0xFFBBCA6F),
    bench = Color(0xFFE9E29B),
    surface = Color(0xFFF2CFBB),
    surfaceSoft = Color(0xFFF1B4AF),
    ink = Color(0xFF27270F),
    inkSoft = Color(0xFF6A452F),
    inkFaint = Color(0xFF8E9546),
    inkLine = Color(0xFF27270F),
    leaf = Color(0xFF8E9546),
    leafText = Color(0xFF555A1B),
    leafTint = Color(0xFFBBCA6F),
    onLeaf = Color(0xFF27270F),
    berry = Color(0xFFF28892),
    berryTint = Color(0xFFF1B4AF),
    butter = Color(0xFFE9E29B),
    butterTint = Color(0xFFF2CFBB),
    onButter = Color(0xFF27270F),
    jam = Color(0xFFA9303D),
    jamText = Color(0xFFA9303D),
    onJam = Color(0xFFFFF9DB),
    hair = Color(0x2927270F),
    shade = Color(0x1227270F),
    softShadow = Color(0x4D27270F),
    contact = Color(0x3327270F),
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
    paper = Color(0xFFFFF6E8),
    paperEdge = Color(0xFFEFDFC6),
    desk = Color(0xFF242312),
    deskEdge = Color(0xFF323119),
    bench = Color(0xFF242312),
    surface = Color(0xFF3D3920),
    surfaceSoft = Color(0xFF46352E),
    ink = Color(0xFFFFF9DB),
    inkSoft = Color(0xFFDAD7A0),
    inkFaint = Color(0xFF8E9546),
    inkLine = Color(0xFF120E0A),
    leaf = Color(0xFFBBCA6F),
    leafText = Color(0xFFBBCA6F),
    leafTint = Color(0xFF8E9546),
    onLeaf = Color(0xFF242312),
    berry = Color(0xFFF28892),
    berryTint = Color(0xFFF1B4AF),
    butter = Color(0xFFE9E29B),
    butterTint = Color(0xFF3D3920),
    onButter = Color(0xFF242312),
    jam = Color(0xFFFF9CA4),
    jamText = Color(0xFFFF9CA4),
    onJam = Color(0xFF242312),
    hair = Color(0x26FFF9DB),
    shade = Color(0x1227270F),
    softShadow = Color(0x9E120E0A),
    contact = Color(0x80120E0A),
)

/**
 * `.scrim{background:rgba(39,39,15,.44)}` — the one V2.1 scrim, for **every** surface that dims what is
 * behind it. `.44 × 255 = 112 = 0x70`.
 *
 * It is deliberately theme-invariant: the palette amendment derives it from the source's lit ink and
 * shares it across every modal surface so identical overlays never dim the studio differently.
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
public val ZinelyV21Scrim: Color = Color(0x7027270F)
